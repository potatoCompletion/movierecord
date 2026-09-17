# 인증 설계

> [← README](../README.md)

폼 로그인과 OAuth2 소셜 로그인을 하나의 필터 체인에서 처리하고, HTTP 세션 없이 JWT 액세스 토큰과 서버 보관 리프레시 토큰으로 인증 상태를 유지합니다. 관련 패키지는 `auth/`(security, handler, oauth, service, domain)와 `config/SecurityConfig`입니다.

## 1. Spring Security — 폼 로그인과 OAuth2 이중 인증 체계

**설계 목표**

폼 로그인 전용 계정과 소셜 로그인 전용 계정을 하나의 `users` 테이블에서 관리하면서, 계정 상태(PENDING / ACTIVE / WITHDRAWN)에 따른 접근 제어를 두 인증 경로 모두에 일관되게 적용합니다.

**상태 흐름**

```
회원가입 → PENDING
              ↓ 관리자 승인
           ACTIVE ←── 복구
              ↓ 탈퇴
          WITHDRAWN
```

**폼 로그인 경로**

`UserService`가 `UserDetailsService`를 구현합니다. `loadUserByUsername()`에서 계정 상태에 따라 Spring Security 표준 예외를 던집니다.

- `PENDING` → `DisabledException` → 로그인 실패 핸들러가 `/auth/login?disabled`로 리다이렉트
- `WITHDRAWN` → `LockedException` → `/auth/login?withdrawn`으로 리다이렉트
- OAuth2 전용 계정(`provider != null`) → `UsernameNotFoundException`으로 폼 로그인 자체를 차단. 비밀번호 필드에는 `{noop}OAUTH_ACCOUNT_NO_PASSWORD` 센티넬 값을 저장해 실수로 인증이 통과되지 않도록 이중 방어

**OAuth2 경로**

`CustomOAuth2UserService`가 `OAuth2UserService`를 구현합니다. `loadUser()`에서 소셜 사용자를 DB에 등록하거나 조회한 뒤, 상태가 PENDING·WITHDRAWN이면 커스텀 `errorCode`를 담은 `OAuth2AuthenticationException`을 던집니다.

```java
// CustomOAuth2UserService.loadUser()
if (user.getStatus() == UserStatus.PENDING) {
    throw new OAuth2AuthenticationException(
        new OAuth2Error("account_pending", "Account is awaiting admin approval", null));
}
```

`OAuth2LoginFailureHandler`는 `errorCode`를 Java 21 switch expression으로 분기해 동일한 로그인 페이지 파라미터로 연결합니다.

```java
url = switch (oae.getError().getErrorCode()) {
    case "account_pending"  -> "/auth/login?disabled";
    case "account_withdrawn" -> "/auth/login?withdrawn";
    default                 -> "/auth/login?error";
};
```

**핸들러 분리**

성공·실패 후처리 로직을 `LoginSuccessHandler`, `OAuth2LoginSuccessHandler`, `LoginFailureHandler`, `OAuth2LoginFailureHandler` 네 클래스로 분리하고 `SecurityConfig`에 주입했습니다. 두 성공 핸들러는 동일하게 토큰 쌍을 발급해 쿠키로 내려주고, `SecurityConfig`가 핸들러의 구현 방식을 몰라도 되도록 인터페이스 타입(`AuthenticationSuccessHandler`, `AuthenticationFailureHandler`)으로 의존합니다.

**결과**

폼 로그인·소셜 로그인 어느 경로로 시도해도 PENDING·WITHDRAWN 계정은 동일한 안내 메시지 화면으로 리다이렉트됩니다.

**데모 계정**

운영에서는 `.env`의 `DEMO_PASSWORD`가 설정되어 있을 때만 기동 시 `ProdDataInitializer`가 `demo` 계정(ROLE_USER, ACTIVE)을 시딩합니다. 값이 없으면 경고 로그만 남기고 건너뛰며, 이미 존재하면 다시 만들지 않습니다. 반면 관리자 계정은 `ADMIN_PASSWORD`가 없으면 기동 자체를 실패시킵니다. 데모는 있으면 좋은 것이고 관리자는 없으면 운영이 불가능하기 때문입니다. 승인 절차 없이 감상 기록과 마이페이지 통계를 바로 체험하기 위한 계정입니다.

## 2. 토큰 기반 stateless 인증 — JWT 액세스 토큰 + 서버 보관 리프레시 토큰

**설계 목표**

HTTP 세션 없이 인증 상태를 유지합니다. 서버가 세션을 들고 있지 않으므로 인스턴스를 교체하거나 재시작해도 로그인이 유지되고, 컨테이너 메모리에 세션 저장소가 필요 없습니다. 대신 "서버가 로그아웃을 강제할 수 없다"는 stateless JWT의 약점을 서버 보관 리프레시 토큰 원장으로 보완합니다.

**토큰 구조**

| 토큰 | 형식 | 보관 | TTL |
|------|------|------|-----|
| 액세스 토큰 | JWT (HS256) | httpOnly 쿠키 `ACCESS_TOKEN`, `Path=/` | 15분 |
| 리프레시 토큰 | 32바이트 난수 | httpOnly 쿠키 `REFRESH_TOKEN`, `Path=/auth` + DB(SHA-256 해시) | 14일 |

`JwtAuthenticationFilter`가 매 요청 `ACCESS_TOKEN` 쿠키를 검증해 SecurityContext를 재구성합니다(`SessionCreationPolicy.STATELESS`). 두 쿠키 모두 `HttpOnly`, `SameSite=Lax`이고 운영에서는 `Secure`를 강제합니다(`app.cookie.secure`). 리프레시 쿠키는 `Path=/auth`로 스코프를 좁혀 토큰 재발급·로그아웃 요청에만 전송됩니다. 서명 시크릿(`APP_JWT_SECRET`)은 환경 변수로만 주입하며, 누락되거나 32바이트 미만이면 기동 시 fail-fast 합니다.

**리프레시 토큰 원장과 Redis 캐시**

리프레시 토큰은 원문을 저장하지 않고 SHA-256 해시만 `refresh_tokens` 테이블에 보관합니다. DB가 유출돼도 원문을 복원할 수 없습니다. Redis에는 해시 → userId 조회 캐시를 write-through로 함께 기록하되, DB가 유일한 원장(source of truth)이고 Redis는 best-effort입니다.

Redis 접근은 별도 서킷브레이커(`refreshTokenCache`)로 감쌌습니다. 외부 API용 기본 설정은 HTTP 예외만 집계하므로 이 인스턴스는 `record-exceptions`를 재정의해 Redis 장애가 집계되도록 했고, Redis 호출은 외부 API보다 훨씬 빨라야 하므로 open-state 대기를 5초로 짧게 잡았습니다. 서킷이 열리거나 Redis가 죽으면 `TokenService`가 예외를 삼키고 DB 원장으로 폴백해 로그인 유지에는 영향이 없습니다.

```java
// TokenService — Redis 캐시는 best-effort, 실패 시 DB 원장으로 폴백
private Optional<Long> lookupCache(String hash) {
    try {
        return cacheService.findUserId(hash);
    } catch (Exception e) {
        log.warn("Redis 리프레시 캐시 조회 실패 → DB 폴백: {}", e.toString());
        return Optional.empty();
    }
}
```

**회전(rotation)과 재사용 탐지**

`POST /auth/token/refresh`는 리프레시 토큰을 일회용으로 소비하고 새 액세스·리프레시 쌍을 발급합니다. 회전 체인은 `familyId`로 묶이며, 이미 폐기된 토큰이 다시 제출되면 탈취로 간주해 같은 family 전체를 폐기합니다. 회전할 때마다 사용자 상태를 DB에서 재확인하므로 탈퇴 처리가 다음 재발급 시점에 반영됩니다.

같은 리프레시 토큰으로 두 요청이 동시에 들어오면 "조회 후 폐기" 방식은 둘 다 통과시킵니다(TOCTOU). `WHERE revoked_at IS NULL` 조건부 UPDATE 하나로 소비를 원자화해, 정확히 한 요청만 1행을 갱신하고 나머지는 0행을 받아 재사용 탐지로 흘러갑니다. 이 동작은 `TokenServiceConcurrencyTest`로 검증합니다.

```java
// RefreshTokenRepository — 원자적 one-time-use 소비
@Modifying
@Query("update RefreshToken r set r.revokedAt = :now where r.tokenHash = :hash and r.revokedAt is null")
int revokeIfActive(@Param("hash") String hash, @Param("now") LocalDateTime now);
```

**강제 로그아웃의 트레이드오프**

관리자가 회원을 강제 탈퇴시키면 `TokenService.revokeAllForUser()`로 해당 사용자의 리프레시 토큰을 모두 폐기해 재발급을 차단합니다. 이미 발급된 액세스 토큰은 서버가 회수할 수 없으므로 최대 15분간 유효합니다. 액세스 토큰 TTL을 짧게 잡은 이유이며, 즉시 차단이 필요해지면 블랙리스트를 추가하는 선택지를 남겨 두었습니다.

**세션이 생기지 않도록 한 부수 작업**

STATELESS로 바꿔도 Spring Security의 기본 구성 요소 몇 가지가 여전히 세션을 만들어 `JSESSIONID`가 발급되는 문제가 있었습니다. 세션 의존 지점을 모두 쿠키 기반으로 교체했습니다.

| 세션 사용 지점 | 교체 |
|---------------|------|
| OAuth2 인가 요청(state, nonce) 저장소 | `CookieOAuth2AuthorizationRequestRepository` — JSON → Base64URL + HMAC-SHA256 서명 쿠키, 10분 만료 |
| CSRF 토큰 저장소 | `CookieCsrfTokenRepository` |
| 리다이렉트 flash 속성 | `CookieFlashMapManager` |

CSRF 쿠키 전환 후에는 JWT 필터가 인증을 채운 매 요청마다 기본 `CsrfAuthenticationStrategy`가 `XSRF-TOKEN` 쿠키를 삭제해, JSON 응답 뒤의 폼 POST가 403이 되는 회귀가 있었습니다. 세션이 없어 토큰 고정(fixation) 방어가 의미 없으므로 `NullAuthenticatedSessionStrategy`로 교체했고, `SecurityConfigCsrfCookieTest`로 재발을 막고 있습니다. 두 문제의 진단 과정은 [트러블슈팅](troubleshooting.md)에 있습니다.
