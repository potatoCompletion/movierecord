# MovieRecord

![CI](https://github.com/potatoCompletion/movierecord/actions/workflows/ci.yml/badge.svg?branch=master)

영화·TV 시리즈 감상 기록 웹 서비스. TMDB·KOBIS·OMDb 세 API를 연동해 홈 화면을 구성하고, 통합 검색으로 작품을 찾아 별점·감정·몰입감·스토리·취향 일치도를 기록합니다. 마이페이지에서 감상 통계를 확인할 수 있습니다.

> 개인 프로젝트 | Java 21 / Spring Boot | **서비스: [mu-ra-bel.com](https://mu-ra-bel.com)**
> 데모 계정: `demo` / `demo`

![홈 화면](docs/images/home.png)

### 핵심 설계 결정

- **세션 없는 인증**: JWT 액세스 토큰 + 서버 보관 리프레시 토큰. 리프레시 토큰은 SHA-256 해시로 DB에 두고 회전·재사용 탐지로 탈취에 대응하며, 동시 회전 요청은 조건부 UPDATE 하나로 하나만 통과시킵니다. → [docs/auth.md](docs/auth.md#2-토큰-기반-stateless-인증--jwt-액세스-토큰--서버-보관-리프레시-토큰)
- **화면 중요도에 따른 장애 격리**: 외부 API 3종을 Resilience4j로 감싸되, 핵심 콘텐츠는 503으로 명확히 실패시키고 부가 영역은 빈 결과를 반환해 화면은 그대로 렌더링합니다. → [docs/resilience.md](docs/resilience.md)
- **관측 데이터를 서버 외부로 분리**: 같은 인스턴스에 있던 Prometheus·Grafana를 걷어내고 CloudWatch로 지표·로그·알람을 분리했습니다. 서버가 죽어도 마지막 상태가 남습니다. → [docs/monitoring.md](docs/monitoring.md)
- **포스터 이미지를 CDN에서 직접 서빙**: 디스크 캐싱을 제거하고 TMDB 경로만 저장해 브라우저가 CDN에서 직접 받도록 바꿨습니다. 인스턴스 교체 시 캐시 유실과 이미지 트래픽 부담이 사라졌습니다. → [docs/external-api.md](docs/external-api.md#4-이미지-서빙--tmdb-cdn-직접-참조)

## 문서

상세 설계와 운영 기록은 `docs/`에 있습니다. README는 요약만 담습니다.

| 문서 | 내용 |
|------|------|
| [docs/features.md](docs/features.md) | 기능별 상세 설명과 스크린샷 |
| [docs/auth.md](docs/auth.md) | 폼·OAuth2 이중 인증, JWT + 리프레시 토큰 원장, 세션 제거 |
| [docs/external-api.md](docs/external-api.md) | OMDb 평점, KOBIS + TMDB 포스터 합성, 한국 개봉일 D-Day, CDN 이미지 서빙 |
| [docs/resilience.md](docs/resilience.md) | Resilience4j 4중 방어, 중요도별 장애 대응, 예외 분류 |
| [docs/cache.md](docs/cache.md) | Redis 캐시 TTL, 사전 워밍, 캐시 키 버저닝 |
| [docs/architecture.md](docs/architecture.md) | 서버 구성, ARM64 전환, 메모리 상한, 기술 스택 상세, 패키지 구조 |
| [docs/testing.md](docs/testing.md) | 테스트 계층, 설계 결정 검증 테스트, CI 단계 |
| [docs/monitoring.md](docs/monitoring.md) | CloudWatch 지표·로그·헬스 체크·알람 |
| [docs/deploy.md](docs/deploy.md) | CI와 Docker 빌드 역할, 배포 절차, Flyway 마이그레이션 |
| [docs/troubleshooting.md](docs/troubleshooting.md) | 운영 중 겪은 문제와 해결 |

---

## 기술 스택

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.0.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=flat-square&logo=thymeleaf&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL_8.4-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)
![AWS](https://img.shields.io/badge/AWS_EC2-FF9900?style=flat-square&logo=amazonec2&logoColor=white)
![CloudWatch](https://img.shields.io/badge/CloudWatch-FF4F8B?style=flat-square&logo=amazoncloudwatch&logoColor=white)
![Resilience4j](https://img.shields.io/badge/Resilience4j-121212?style=flat-square&logo=resilience4j&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)

| 분류 | 기술 | 선택 이유 |
|------|------|---------|
| Backend | Java 21, Spring Boot 4.0.5, Spring Data JPA, Thymeleaf | Record·패턴 매칭으로 간결한 DTO·분기, 서버 사이드 렌더링으로 별도 API 서버 없이 구현 |
| Security | Spring Security + JWT (jjwt) | 폼 로그인과 OAuth2를 한 필터 체인에서 통합. 세션 없는 stateless 인증, 리프레시 토큰은 DB 원장 + Redis 캐시 |
| Data | MySQL 8.4 (운영) / H2 (로컬·테스트), Flyway, Redis | 스키마는 버전 관리되는 마이그레이션으로만 변경, TTL 기반 캐싱으로 외부 API 호출 절감 |
| Resilience | Resilience4j | 외부 API 3종 장애가 전체 서비스로 전파되지 않도록 격리·차단 |
| Infra | AWS EC2 (t4g.small / ARM64), Docker Compose, Nginx, GitHub Actions | 컨테이너 단위 배포, 리버스 프록시로 SSL 종단과 앱 분리, push·PR마다 테스트 자동 실행 |
| Observability | AWS CloudWatch + Actuator | 관측 데이터를 인스턴스 외부에 적재해 서버가 죽어도 마지막 상태가 남도록 구성 |

---

## 주요 기능

상세 설명과 화면별 스크린샷은 [docs/features.md](docs/features.md)에 있습니다.

- **스포트라이트**: 매일 TMDB에서 무작위로 고른 영화를 OMDb 평점(IMDb·RT·Metacritic)으로 검증해 3편을 홈 히어로에 표시. 전날 23:00에 익일분을 사전 워밍
- **박스오피스 TOP 10**: KOBIS 전일 일별 순위에 TMDB 포스터를 매칭한 캐러셀
- **곧 개봉해요**: 3주 내 한국 극장 개봉작을 한국 개봉일 기준 D-Day로 표시, 재개봉작은 뱃지로 구분
- **통합 검색과 상세 페이지**: 영화·TV·인물을 한 번에 검색하고, 작품 페이지에는 TMDB·OMDb 평점 배지와 사용자 평균 별점을 함께 표시
- **감상 기록**: 별점, 한줄평, 몰입감, 스토리, 감정, 취향 일치도를 기록
- **마이페이지 통계**: 총·연간·월간 기록 수, 평균 별점, 취향 일치율, 월별 그래프, 감정 분포 차트
- **인증과 관리자**: 폼 로그인 + Google·Naver OAuth2, 관리자 승인제(PENDING → ACTIVE), 강제 탈퇴 시 리프레시 토큰 일괄 폐기

![감상 기록 작성](docs/images/record-form.png)
![마이페이지](docs/images/mypage.png)

---

## 서버 아키텍처

AWS EC2 단일 인스턴스 위에서 Docker Compose로 Nginx · Spring Boot · MySQL · Redis 네 컨테이너를 운영하고, 지표·로그는 인스턴스 외부의 CloudWatch에 적재합니다. 구성 요소, ARM64 전환, 컨테이너 메모리 상한, 기술 스택 상세, 패키지 구조는 [docs/architecture.md](docs/architecture.md)에 있습니다.

```
                        Internet
                            │
                            ▼
                  Elastic IP (고정 IP)
                            │
                HTTP :80 / HTTPS :443
                            │
 ┌──────────────────────────┼─────────────────────────────┐
 │  AWS EC2 t4g.small (ARM64 / Amazon Linux 2023)         │
 │  ┌───────────────────────┼───────────────────────────┐ │
 │  │  Docker Compose Network                           │ │
 │  │                       ▼                           │ │
 │  │              ┌─────────────┐                      │ │
 │  │              │    Nginx    │  mem_limit 64m       │ │
 │  │              │  :80 / :443 │                      │ │
 │  │              └──────┬──────┘                      │ │
 │  │                     │ proxy_pass                  │ │
 │  │                     │ http://app:8080             │ │
 │  │                     ▼                             │ │
 │  │              ┌──────────────┐                     │ │
 │  │              │ Spring Boot  │  mem_limit 900m     │ │
 │  │              │ :8080  (app) │  -Xmx450m           │ │
 │  │              │ :9090  (관리)│                     │ │
 │  │              └──────┬───────┘                     │ │
 │  │                     │ JDBC / Redis                │ │
 │  │              ┌──────┴──────┐                      │ │
 │  │              ▼             ▼                      │ │
 │  │       ┌────────────┐ ┌────────────┐               │ │
 │  │       │ MySQL 8.4  │ │   Redis    │               │ │
 │  │       │ 127.0.0.1  │ │ 127.0.0.1  │               │ │
 │  │       │   :3306    │ │   :6379    │               │ │
 │  │       │   400m     │ │    64m     │               │ │
 │  │       └────────────┘ └────────────┘               │ │
 │  └───────────────────────────────────────────────────┘ │
 │         │ 호스트 지표 (CloudWatch Agent)                │
 │         │ 컨테이너 로그 (awslogs 드라이버)              │
 └─────────┼───────────────────────────────────────────────┘
           ▼
    AWS CloudWatch ──→ SNS ──→ 이메일 알림
```

---

## 트러블슈팅

운영하면서 실제로 겪은 문제와 해결 과정입니다. 각 항목은 [docs/troubleshooting.md](docs/troubleshooting.md)에서 증상·원인·해결 순으로 정리했습니다.

- [stateless 설정인데 JSESSIONID가 발급됨](docs/troubleshooting.md#stateless-설정인데-jsessionid가-발급됨)
- [JSON 응답 뒤 폼 POST가 403](docs/troubleshooting.md#json-응답-뒤-폼-post가-403)
- [리프레시 토큰 동시 회전 레이스](docs/troubleshooting.md#리프레시-토큰-동시-회전-레이스)
- [박스오피스 포스터가 엉뚱한 작품으로 매칭](docs/troubleshooting.md#박스오피스-포스터가-엉뚱한-작품으로-매칭)
- ["곧 개봉해요"에 음수 D-Day가 표시됨](docs/troubleshooting.md#곧-개봉해요에-음수-d-day가-표시됨)
- [배포 직후 홈 화면 500](docs/troubleshooting.md#배포-직후-홈-화면-500)
- [로그 수집이 애플리케이션을 멈출 수 있는 구조](docs/troubleshooting.md#로그-수집이-애플리케이션을-멈출-수-있는-구조)
- [H2로는 잡히지 않는 MySQL 전용 마이그레이션 오류](docs/troubleshooting.md#h2로는-잡히지-않는-mysql-전용-마이그레이션-오류)

---

## 테스트와 CI

테스트 클래스 34개, 테스트 164개. `./gradlew test`로 전부 실행되며 외부 API·Redis·MySQL 없이 H2와 목만으로 돌아갑니다. 스키마는 테스트에서도 Flyway 마이그레이션으로 만들어 마이그레이션 파일과 엔티티의 불일치를 바로 잡아냅니다. master push와 PR마다 GitHub Actions([ci.yml](.github/workflows/ci.yml))가 테스트, bootJar, Docker 빌드를 실행하고 master는 CI 통과를 머지 조건으로 둡니다.

동작만이 아니라 설계 결정이 실제로 지켜지는지를 테스트로 검증합니다. 중요도별 장애 대응, Resilience4j 프록시 동작, 리프레시 토큰 one-time-use 동시성, Redis 장애 시 DB 폴백, CSRF 쿠키 회귀, 서명 쿠키 위변조, 음수 D-Day 회귀가 대표적입니다. 계층별 구성과 테스트 목록은 [docs/testing.md](docs/testing.md)에 있습니다.

---

## 로컬 실행

프로젝트 루트에 `.env.properties` 파일을 만듭니다(`.gitignore` 대상). `local` 프로파일이 `spring.config.import`로 이 파일을 읽으므로, 같은 값을 환경 변수로 넘겨도 됩니다.

```properties
TMDB_API_TOKEN=eyJ...          # Bearer 접두사 없이 토큰만 (클라이언트가 붙임)
KOBIS_API_KEY=your_key
OMDB_API_KEY=your_key
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...
```

JWT 서명 시크릿(`APP_JWT_SECRET`)은 로컬 프로파일에 개발용 기본값이 있어 생략할 수 있습니다. 운영에서는 32바이트 이상 값을 반드시 주입해야 하며, 없으면 기동에 실패합니다.

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

`local` 프로파일은 H2 파일 DB(`./data/`)와 인메모리 캐시를 사용하므로 MySQL·Redis 없이 실행됩니다. 스키마는 Flyway가 만들고, `admin` / `admin1234` 계정이 시딩됩니다. Flyway 도입 이전에 만든 `./data/`가 있으면 기동이 실패하므로 먼저 삭제합니다(`rm -rf ./data`).

| 항목 | 값 |
|------|---|
| H2 콘솔 | http://localhost:8080/h2-console |
| JDBC URL | `jdbc:h2:file:./data/movierecord;AUTO_SERVER=TRUE;MODE=MySQL` |
| 사용자 | `sa` |
| 비밀번호 | (없음) |

운영 배포와 스키마 마이그레이션 절차는 [docs/deploy.md](docs/deploy.md)에 있습니다.

---
