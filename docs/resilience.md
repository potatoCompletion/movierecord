# 장애 격리 — Resilience4j

> [← README](../README.md)

**설계 목표**

이 서비스는 홈 화면 하나를 그리는 데도 TMDB·KOBIS·OMDb 세 외부 API에 의존합니다. 특정 API가 느려지거나 죽었을 때 스레드가 묶여 전체 서비스가 함께 멈추는 상황(장애 전파)을 막는 것이 목표입니다. 설정값은 `application.properties`의 `resilience4j.*`에 있습니다.

## 4중 방어 데코레이터

모든 외부 API 호출 메서드를 Resilience4j 애너테이션으로 감쌌습니다. 실행 순서는 `Bulkhead → RateLimiter → CircuitBreaker → Retry`입니다. KOBIS는 호출량이 하루 한 번 수준이라 RateLimiter를 제외한 3중으로 적용했습니다.

```java
// TmdbClient — 모든 public 호출에 동일한 방어 스택 적용
@Bulkhead(name = "tmdbApi")          // ① 동시 호출 상한 → 스레드 고갈 격리
@RateLimiter(name = "tmdbApi")       // ② 초당 호출 상한 → API 쿼터 보호
@CircuitBreaker(name = "tmdbApi")    // ③ 실패율 초과 시 차단 → 죽은 API로의 호출 조기 차단
@Retry(name = "tmdbApi", fallbackMethod = "searchMultiFallback")  // ④ 일시 장애 재시도
public List<TmdbSearchItem> searchMulti(String query) { ... }
```

| 방어 | 역할 | 핵심 설정 |
|------|------|----------|
| Bulkhead | 외부 API별 동시 호출 수를 제한해 자원을 격리 | tmdbApi 20 / omdbApi 10 / kobisApi 5 |
| RateLimiter | API 제공사 쿼터를 넘지 않도록 호출량 제한 | tmdbApi 40 req/s, omdbApi 900 req/day |
| CircuitBreaker | 최근 20건 중 실패율 50% 초과 시 OPEN, 10초 후 Half-Open으로 자동 복구 | slow-call 4s 초과도 실패로 집계 |
| Retry | 일시적 오류를 지수 백오프로 재시도 | 최대 3회, 300ms → 2배 증가 |

## 중요도별 장애 대응 정책

장애 시 어떻게 대응할지를 화면 중요도에 따라 두 갈래로 나눴습니다.

- **핵심 콘텐츠**(검색 결과, 영화/TV/인물 상세): fallback 없이 예외를 전파해 상위에서 503 에러 페이지로 표면화 — 잘못된 정보를 보여주느니 실패를 명확히 드러냄
- **부가 영역**(자동완성, 홈 캐러셀, 박스오피스 포스터 보강, 작품별 한국 개봉일 조회): `fallbackMethod`로 빈 결과·null을 반환 — 부가 기능 하나가 죽어도 홈은 정상 렌더링

같은 클래스 안에서 다른 메서드를 직접 호출하면 AOP 프록시를 거치지 않아 데코레이터가 적용되지 않습니다. 예를 들어 "곧 개봉해요"의 작품별 개봉일 조회는 `TmdbClient` 내부가 아니라 `TmdbHomeService`가 호출하도록 두어 각 호출이 독립적으로 보호됩니다.

## 예외 분류로 재시도·차단 대상 제어

무엇을 "장애"로 볼지 예외 타입으로 구분했습니다. 재시도해도 소용없는 4xx(존재하지 않는 리소스 등)까지 재시도·서킷에 집계하면 오히려 정상 트래픽을 오판할 수 있기 때문입니다.

| 예외 | 성격 | Retry / CircuitBreaker |
|------|------|------------------------|
| `ExternalApiTransientException` | 5xx·타임아웃 등 일시 장애 | **재시도 O / 실패로 집계** |
| `ExternalApiClientException` | 4xx 등 재시도가 무의미한 오류 | **재시도 X / 무시** |

## 전송 타임아웃 설정

Resilience4j 이전에, RestClient 자체에 connect/read 타임아웃을 걸어 무한 대기를 원천 차단했습니다.

| 클라이언트 | Connect | Read | 비고 |
|-----------|---------|------|------|
| TMDB API | 3s | 5s | `config/TmdbConfig` |
| OMDb | 3s | 5s | `omdb/config/OmdbConfig` |
| KOBIS | — | — | 공식 SDK라 전송 타임아웃 제어 불가 → **Bulkhead로 자원 격리**로 대체 방어 |

## Redis 캐시 서킷브레이커

리프레시 토큰 조회 캐시(Redis)는 별도 인스턴스 `refreshTokenCache`로 보호합니다. 외부 API용 기본 설정은 HTTP 예외만 집계하므로 `record-exceptions`를 `java.lang.Exception`으로 재정의했고, open-state 대기를 5초로 짧게 잡았습니다. 서킷이 열리면 `TokenService`가 DB 원장으로 폴백합니다. 자세한 내용은 [auth.md](auth.md#2-토큰-기반-stateless-인증--jwt-액세스-토큰--서버-보관-리프레시-토큰)에 있습니다.

## 검증 테스트

- `ExternalClientResilienceTest`: RestClient 빈을 항상 전송 오류를 내는 스텁으로 바꿔, 부가 호출은 재시도 소진 후 빈 결과를 반환하고 핵심 호출은 예외를 전파하는지 검증합니다.
- `KobisClientResilienceTest`: 벤더 SDK를 목으로 대체해 재시도 횟수와 최종 fallback을 확인합니다. 프록시 설정이 빠지면 바로 실패합니다.
