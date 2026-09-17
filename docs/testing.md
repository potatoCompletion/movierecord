# 테스트와 CI

> [← README](../README.md)

테스트 클래스 34개, 테스트 164개. `./gradlew test`로 전부 실행되며 외부 API·Redis·MySQL 없이 H2와 목만으로 돌아갑니다. 스키마는 테스트에서도 Flyway 마이그레이션으로 만들어 마이그레이션 파일과 엔티티의 불일치를 바로 잡아냅니다.

## 계층별 구성

| 계층 | 도구 | 대상 |
|------|------|------|
| 단위 | JUnit 5 + Mockito | 토큰 발급·회전·폐기, JWT 파싱, 통계 계산기, 이미지 URL 조립, OAuth 속성 매핑, 캐시 설정, 한국 개봉일 선택·D-Day 계산 |
| 컨트롤러 | `@WebMvcTest` + 실제 `SecurityConfig`, standalone MockMvc | 인증·기록·마이페이지·홈 컨트롤러의 인가 규칙과 뷰 렌더링 |
| 리포지토리 | `@DataJpaTest` | 감상 기록 조회 쿼리, JPA Auditing |
| 통합 | `@SpringBootTest` | Resilience4j 데코레이터, 리프레시 토큰 캐시, 동시 회전, 운영 데이터 시딩, 컨텍스트 로딩 |

테스트 프로파일(`src/test/resources/application-test.properties`)은 더미 API 키와 JWT 시크릿을 제공하고, 컨텍스트마다 다른 이름의 인메모리 H2(`MODE=MySQL`)를 씁니다. `@DataJpaTest`가 MODE 없는 임베디드 DB로 바꾸지 않도록 `spring.test.database.replace=none`을 둡니다.

## 설계 결정을 검증하는 테스트

동작만이 아니라 문서에 적은 설계 결정이 실제로 지켜지는지를 테스트로 검증합니다.

- **중요도별 장애 대응** — `ExternalClientResilienceTest`: RestClient 빈을 항상 전송 오류를 내는 스텁으로 바꿔 놓고, 부가 호출(홈 캐러셀, OMDb 평점, 작품별 한국 개봉일)은 재시도 소진 후 빈 결과·null을 반환하고 핵심 호출(영화 상세)은 예외를 전파하는지 각각 검증합니다.
- **Resilience4j 애너테이션이 실제로 동작하는지** — `KobisClientResilienceTest`: 벤더 SDK를 목으로 대체해 재시도 횟수와 최종 fallback을 확인합니다. 프록시 설정이 빠지면 바로 실패합니다.
- **리프레시 토큰 one-time-use** — `TokenServiceConcurrencyTest`: 같은 토큰으로 스레드 2개를 `CountDownLatch`로 동시에 출발시켜 정확히 하나만 회전에 성공하고 나머지는 재사용 탐지로 실패하는지 검증합니다.
- **Redis 장애 시 DB 폴백** — `TokenServiceTest.refresh_redisDown_fallsBackToDb`: 캐시 서비스가 예외를 던져도 회전이 DB 원장만으로 완료되는지 확인합니다.
- **CSRF 쿠키 회귀** — `SecurityConfigCsrfCookieTest`: JWT 인증 요청이 기존 `XSRF-TOKEN` 쿠키를 삭제하지 않는지 검증합니다.
- **서명 쿠키 위변조** — `CookieOAuth2AuthorizationRequestRepositoryTest`: 서명이 변조되거나 다른 키로 서명된 OAuth2 인가 요청 쿠키를 무시하는지 검증합니다.
- **음수 D-Day 회귀** — `HomeControllerRenderTest`, `HomeControllerTest`, `UpcomingCardTest`: 재개봉작이 "재개봉" 뱃지와 한국 개봉일 기준 D-Day로 렌더되고 `D--` 문자열이 나오지 않는지(`HomeControllerRenderTest`), 요청 시각 기준으로 ddays가 계산되고 개봉일이 지난 항목이 제외되는지(`HomeControllerTest`, `UpcomingCardTest`) 검증합니다.
- **운영 데이터 시딩** — `ProdDataInitializerTest`: `DEMO_PASSWORD`가 있을 때만 `demo` 계정이 ACTIVE/ROLE_USER로 생성되고, 없으면 예외 없이 건너뛰며, 이미 있으면 중복 생성하지 않는지, 반대로 `ADMIN_PASSWORD`가 없으면 기동 자체를 실패시키는지 검증합니다.

## CI

master push, PR, 수동 실행 시 GitHub Actions([`.github/workflows/ci.yml`](../.github/workflows/ci.yml))가 다음을 실행합니다.

1. temurin 21 + Gradle 캐시 준비
2. `./gradlew test` — 실패해도 `build/reports/tests`, `build/test-results`를 아티팩트로 업로드
3. `./gradlew bootJar`
4. `docker build`(push 없음) — Dockerfile이 현재 소스로 빌드되는지 확인

시크릿은 필요 없습니다. master는 브랜치 보호 규칙으로 직접 push가 막혀 있고 `build` job 통과가 머지 조건입니다. 서버에서 Docker 이미지를 빌드할 때는 테스트가 CI에서 끝났다는 전제로 `-x test`를 씁니다([deploy.md](deploy.md#ci와-docker-빌드의-역할-분담)).
