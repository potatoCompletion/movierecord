# 트러블슈팅

> [← README](../README.md)

운영하면서 실제로 겪은 문제와 해결 과정입니다. 각 항목의 설계 배경은 링크된 문서에 있습니다.

## stateless 설정인데 JSESSIONID가 발급됨

- **증상**: `SessionCreationPolicy.STATELESS`로 바꿨는데 소셜 로그인을 시작하면 `JSESSIONID` 쿠키가 생김.
- **원인**: 기본 `HttpSessionOAuth2AuthorizationRequestRepository`가 소셜 로그인 시작 시, 기본 `SessionFlashMapManager`가 리다이렉트 flash 저장 시 각각 HttpSession을 만들고 있었음. 세션 정책은 "인증 정보를 세션에 두지 않는다"는 뜻이지 "세션을 만들지 않는다"는 뜻이 아님.
- **해결**: 두 지점을 쿠키 기반 구현으로 교체. 인가 요청은 JWT 시크릿으로 HMAC 서명한 쿠키, flash 속성은 짧은 수명의 `CookieFlashMapManager`. → [세션이 생기지 않도록 한 부수 작업](auth.md#2-토큰-기반-stateless-인증--jwt-액세스-토큰--서버-보관-리프레시-토큰)

## JSON 응답 뒤 폼 POST가 403

- **증상**: 검색 자동완성(JSON API)을 한 번 호출하고 나면 그 뒤 폼 제출이 CSRF 403.
- **원인**: JWT 필터가 매 요청 인증을 채우자 기본 `CsrfAuthenticationStrategy`가 "인증 시 토큰 회전"을 매 요청 실행. 기존 `XSRF-TOKEN` 쿠키는 삭제되지만 폼을 렌더링하지 않는 JSON 응답은 새 토큰을 내리지 않아 브라우저 쿠키만 사라짐.
- **해결**: 세션이 없어 토큰 고정 방어가 의미 없으므로 `NullAuthenticatedSessionStrategy`로 교체. `SecurityConfigCsrfCookieTest`로 회귀 방지.

## 리프레시 토큰 동시 회전 레이스

- **증상**: 같은 리프레시 토큰으로 두 요청이 거의 동시에 오면 둘 다 새 토큰을 받음. 재사용 탐지가 뚫림.
- **원인**: "조회 → 폐기 여부 확인 → 폐기" 순서가 원자적이지 않음(TOCTOU). 두 트랜잭션이 같은 스냅샷을 읽음.
- **해결**: `WHERE revoked_at IS NULL` 조건부 UPDATE 한 문장으로 소비를 원자화. 갱신 행 수가 0이면 재사용으로 판정. `TokenServiceConcurrencyTest`가 실제 스레드 2개로 검증. → [회전과 재사용 탐지](auth.md#2-토큰-기반-stateless-인증--jwt-액세스-토큰--서버-보관-리프레시-토큰)

## 박스오피스 포스터가 엉뚱한 작품으로 매칭

- **증상**: KOBIS 제목으로 TMDB를 검색하면 동명의 TV 시리즈 포스터가 붙는 경우가 있음.
- **원인**: 멀티 검색(`/search/multi`)은 영화·TV·인물을 섞어 반환하고 첫 결과를 그대로 썼음.
- **해결**: 박스오피스 매칭에는 영화 전용 검색(`/search/movie`)을 쓰도록 엔드포인트를 분리. → [KOBIS + TMDB 포스터 합성](external-api.md#2-kobis-박스오피스--tmdb-포스터-합성)

## "곧 개봉해요"에 음수 D-Day가 표시됨

- **증상**: 홈의 개봉 예정 섹션에 `D--2702` 같은 항목이 표시됨.
- **원인**: TMDB discover는 `region=KR` 조건으로 한국 개봉일을 필터링하면서도 응답의 `release_date`에는 전세계 최초 개봉일을 돌려줌. 재개봉작이 후보에 들어오면 최초 개봉일이 수년 전이라 D-Day가 큰 음수가 됨. 또한 D-Day를 페치 시점에 계산해 하루 캐시하고 있어 자정이 지나면 하루씩 어긋나는 드리프트도 있었음.
- **해결**: 후보마다 `/movie/{id}/release_dates`에서 한국 극장 개봉일(type 3, 없으면 type 2)을 확정하고, 캐시에는 개봉일만 저장한 뒤 D-Day는 요청 시각에 계산. 개봉일이 지난 항목은 제외하고 재개봉작에는 "재개봉" 뱃지를 표시. → [한국 개봉일 기준 D-Day](external-api.md#3-tmdb-곧-개봉해요--한국-개봉일-기준-d-day)

## 배포 직후 홈 화면 500

- **증상**: DTO에 필드를 추가해 배포했더니 홈 화면이 500. 재시작해도 그대로.
- **원인**: Redis 캐시 값이 타입 정보 없는 JSON이라 옛 캐시가 `LinkedHashMap`으로 그대로 복원되고, 새 코드가 없는 필드를 읽음. TTL이 1일이라 하루 동안 지속되는 구조.
- **해결**: 캐시 키에 버전 접두사를 붙여 배포 시 버전만 올리면 옛 캐시를 우회하도록 변경. 이후 포스터 URL을 `/uploads/`에서 CDN으로 바꿀 때(v3), "곧 개봉해요" 캐시 항목의 구조를 바꿀 때(v4)도 같은 기준으로 올렸습니다. → [직렬화와 캐시 키 버저닝](cache.md#직렬화와-캐시-키-버저닝)

## 로그 수집이 애플리케이션을 멈출 수 있는 구조

- **증상**: 아직 장애로 이어지진 않았지만, CloudWatch Logs 전송이 지연되면 컨테이너 stdout 쓰기가 막히는 구조였음.
- **원인**: Docker 로그 드라이버 기본 모드가 blocking.
- **해결**: `mode: non-blocking` + 버퍼 4MB. 버퍼가 차면 로그를 버리고 앱은 계속 동작. 관측 때문에 서비스가 멈추지 않도록 로그 유실을 감수한 선택. → [로그 수집](monitoring.md#로그-수집)

## H2로는 잡히지 않는 MySQL 전용 마이그레이션 오류

- **증상**: Flyway `V1__init.sql`이 로컬 H2(`MODE=MySQL`)와 테스트에서는 통과했지만, 코드 리뷰에서 실제 MySQL에서는 실패하는 구문이 발견됨.
- **원인**: MySQL은 `CREATE TABLE` 안의 인라인 `FOREIGN KEY`에 대해 제약 이름과 같은 이름의 보조 인덱스를 자동 생성함. 뒤이어 같은 이름으로 `CREATE INDEX`를 실행하면 중복 키 이름 오류. H2는 자동 인덱스에 다른 이름을 붙여 이 충돌을 재현하지 못함.
- **해결**: 인덱스를 먼저 만들고 FK는 `ALTER TABLE ... ADD CONSTRAINT`로 붙여 MySQL이 기존 인덱스를 재사용하게 함. H2 호환 모드는 문법 검증에는 유용하지만 MySQL 동작 재현은 아니므로, MySQL 고유 구문은 따로 확인한다는 원칙을 문서화. → [스키마 마이그레이션](deploy.md#스키마-마이그레이션-flyway)
