# 서버 아키텍처

> [← README](../README.md)

AWS EC2 단일 인스턴스 위에서 Docker Compose로 Nginx · Spring Boot · MySQL · Redis 네 컨테이너를 운영합니다. 관측 데이터(지표·로그)는 인스턴스 외부의 CloudWatch에 적재합니다([monitoring.md](monitoring.md)). 배포 절차는 [deploy.md](deploy.md)에 있습니다.

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

포스터 이미지는 서버를 거치지 않고 브라우저가 TMDB CDN에서 직접 내려받습니다([external-api.md](external-api.md#4-이미지-서빙--tmdb-cdn-직접-참조)).

## 구성 요소

| 컴포넌트 | 역할 |
|---------|------|
| AWS EC2 (t4g.small) | ARM64 단일 인스턴스에서 전체 스택 운영 |
| Elastic IP | 고정 IP. 인스턴스를 교체해도 DNS 변경 없이 트래픽을 넘길 수 있음 |
| Nginx | HTTP → HTTPS 리다이렉트, SSL 종단, 리버스 프록시 |
| Spring Boot | 애플리케이션 서버 (외부 포트 미노출, Docker 내부 통신만) |
| MySQL 8.4 | 운영 DB (127.0.0.1 바인딩으로 호스트 외부 접근 차단) |
| Redis | Spring Cache 백엔드 + 리프레시 토큰 조회 캐시 (127.0.0.1 바인딩) |
| Let's Encrypt | Certbot으로 SSL 인증서 발급·갱신 |
| AWS CloudWatch | 지표·로그 수집, 알람 판정 |
| AWS SNS | 알람 발생 시 이메일 발송 |

## 핵심 설계 포인트

**ARM64 인스턴스 채택**

x86 기반 t3.small에서 ARM 기반 t4g.small로 이전했습니다. 같은 사양 대비 시간당 비용이 약 20% 낮고, 버스터블 인스턴스의 baseline CPU가 20%에서 40%로 두 배입니다.

단순 인스턴스 타입 변경으로는 불가능한 작업입니다. 아키텍처가 달라 기존 AMI를 복제할 수 없어 새 인스턴스를 구성하고 애플리케이션 이미지를 ARM64로 다시 빌드했습니다. Nginx · MySQL · Redis는 공식 멀티아키 이미지를 사용하므로 별도 대응이 필요 없었고, 애플리케이션은 `eclipse-temurin:21` 기반이라 재빌드만으로 동작했습니다. 데이터는 `mysqldump`로 이전했고, Elastic IP를 새 인스턴스로 옮겨 DNS 변경 없이 전환했습니다.

**컨테이너별 메모리 상한**

단일 인스턴스에 네 컨테이너가 함께 올라가므로, 한 컨테이너가 호스트 메모리를 독점하지 않도록 상한을 명시했습니다. 상한이 없으면 JVM이 호스트 전체 메모리를 기준으로 힙을 자동 계산하고, MySQL 역시 사용 가능한 만큼 버퍼를 늘려 서로를 밀어냅니다.

| 컨테이너 | mem_limit | 주요 튜닝 |
|---------|-----------|----------|
| Spring Boot | 900m | `-Xmx450m -XX:MaxMetaspaceSize=256m` |
| MySQL | 400m | `--innodb-buffer-pool-size=128M --performance-schema=OFF` |
| Nginx | 64m | — |
| Redis | 64m | — |

JVM 옵션은 `JAVA_TOOL_OPTIONS` 환경 변수로 주입합니다. GC 로그와 `-XX:+HeapDumpOnOutOfMemoryError`도 함께 설정해 메모리 문제 발생 시 원인을 사후 분석할 수 있게 했습니다.

**Actuator 관리 포트 분리**

Actuator는 서비스 포트(8080)가 아닌 9090에만 노출합니다. 9090은 호스트에 publish하지 않고 expose만 하므로 같은 Docker 네트워크의 컨테이너(헬스 체크 스크립트)만 접근할 수 있고, Nginx는 8080만 프록시하므로 공개 도메인에서 `/actuator/**`에 닿지 않습니다.

**HTTPS 강제 + SSL 종단**

Nginx가 80포트의 모든 요청을 443으로 301 리다이렉트하고, Let's Encrypt 인증서로 SSL을 종단합니다. Spring Boot는 `server.forward-headers-strategy=framework`로 `X-Forwarded-Proto` 헤더를 신뢰해 앱 레벨에서도 HTTPS 요청으로 인식하며, 인증 쿠키에 `Secure` 플래그를 강제합니다.

```nginx
# HTTP → HTTPS 리다이렉트
location / {
    return 301 https://$host$request_uri;
}
```

## 기술 스택

| 분류 | 기술 | 선택 이유 |
|------|------|---------|
| Language | Java 21 | Record, 패턴 매칭 등 최신 문법으로 DTO·분기 처리를 간결하게 표현 |
| Framework | Spring Boot 4.0.5 | 의존성 관리와 자동 설정으로 인프라보다 도메인 로직에 집중 |
| ORM | Spring Data JPA / Hibernate | 객체 중심 모델링, JPA Auditing으로 생성·수정 시각 자동 관리. 스키마는 Flyway가 관리하고 Hibernate는 검증만 수행 |
| Security | Spring Security + JWT (jjwt) | 폼 로그인과 OAuth2를 동일한 필터 체인에서 통합 관리. 세션 없는 stateless 인증, 리프레시 토큰은 DB 원장 + Redis 캐시 |
| View | Thymeleaf | 서버 사이드 렌더링, 별도 API 서버 없이 빠른 기능 구현 |
| DB | H2 (로컬·테스트) / MySQL 8.4 (운영), Flyway | 로컬에서 DB 설치 없이 개발, 운영은 MySQL. 로컬 H2는 MySQL 호환 모드로 같은 마이그레이션을 실행 |
| Cache | Redis (운영) / Spring Cache | TTL 기반 캐싱으로 응답 속도 개선 및 반복 연산 비용 절감 |
| Infra | AWS EC2 (t4g.small / ARM64) + Docker + Nginx | 컨테이너 단위 배포, 리버스 프록시로 SSL 종단과 앱 서버 분리 |
| CI | GitHub Actions | push·PR마다 테스트, bootJar, Docker 빌드 실행. master 머지 조건 |
| Resilience | Resilience4j | 외부 API 3종 장애가 전체 서비스로 전파되지 않도록 격리·차단 |
| Observability | AWS CloudWatch + Actuator | 관측 데이터를 인스턴스 외부에 적재해 서버가 죽어도 마지막 상태가 남도록 구성 |

## 패키지 구조

도메인 중심으로 패키지를 구성했습니다. 새 도메인은 최상위에 패키지를 추가하는 방식으로 확장합니다.

```
com.my.movierecord
├── admin/       관리자 — 회원 승인·탈퇴·복구, 리프레시 토큰 일괄 폐기
├── auth/        인증·인가 — JWT 발급·검증, 리프레시 토큰 원장 (domain, security, handler, oauth, service, ...)
├── common/      홈 컨트롤러, 외부 API 예외 분류·전역 예외 처리, 쿠키 기반 FlashMapManager
├── config/      Security, JPA, Web, 스케줄링, Cache, TMDB RestClient, 운영 데이터 시딩 설정
├── content/     영화·TV 상세 페이지 컨트롤러
├── kobis/       KOBIS 박스오피스 API 연동 (client, config, dto, service)
├── movie/       로컬 콘텐츠 레지스트리 — Content 엔티티 (domain, repository, service)
├── mypage/      마이페이지 통계 컨트롤러
├── omdb/        OMDb 평점 API 클라이언트 (client, config, dto)
├── person/      인물 상세 페이지 컨트롤러
├── record/      감상 기록 CRUD + 통계 계산 (stats/)
├── search/      TMDB 통합 검색 컨트롤러
├── spotlight/   일별 스포트라이트 (domain, dto, repository, scheduler, service)
└── tmdb/        TMDB API 클라이언트 (client, config, controller, dto, image, service)
```

스키마는 `src/main/resources/db/migration/`의 Flyway 마이그레이션으로 관리합니다([deploy.md](deploy.md#스키마-마이그레이션-flyway)).
