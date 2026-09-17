# 운영 배포

> [← README](../README.md)

## 개요

AWS EC2 단일 인스턴스 위에서 Docker Compose로 Nginx · Spring Boot · MySQL · Redis 네 컨테이너를 운영합니다. 서버 구성과 설계 포인트는 [architecture.md](architecture.md), 관측은 [monitoring.md](monitoring.md)에 있습니다.

## CI와 Docker 빌드의 역할 분담

- 테스트는 GitHub Actions([`.github/workflows/ci.yml`](../.github/workflows/ci.yml))에서 실행합니다. master push, PR, 수동 실행 시 temurin 21에서 `./gradlew test` → 테스트 리포트 업로드(실패 시에도) → `./gradlew bootJar` → `docker build`(push 없음) 순으로 돌며, 시크릿 없이 H2와 목만으로 완주합니다.
- master는 브랜치 보호 규칙으로 직접 push가 막혀 있고, PR은 CI의 `build` job이 통과해야 머지할 수 있습니다.
- Dockerfile은 `./gradlew clean bootJar -x test`로 이미지를 만듭니다. 테스트는 CI에서 이미 끝났다는 전제이므로 서버에서 이미지를 빌드할 때 반복하지 않습니다.

## 배포 절차

`.env` 파일에 환경 변수를 작성한 뒤 실행합니다. Nginx가 80 포트를 받아 443으로 리다이렉트하고 앱 서버로 프록시합니다. `DEMO_PASSWORD`를 `.env`에 넣으면 기동 시 체험용 `demo` 계정(ROLE_USER, ACTIVE)이 생성되고, 없으면 건너뜁니다.

```bash
git pull
docker compose up -d --build
```

마이그레이션이 포함된 배포는 다음 순서로 진행합니다.

1. 배포 전 전체 백업: `docker compose exec mysql sh -c 'mysqldump --single-transaction -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > backup.sql`
2. `docker compose up -d --build` 로 기동. Flyway가 새 버전을 적용한 뒤 Hibernate가 검증합니다.
3. 적용 결과 확인: `docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SELECT version, description, type, success FROM flyway_schema_history"'`
4. 홈, 로그인, 감상 기록 페이지 스모크 테스트

기동 로그에 "Using MySQL 8.4 which is newer than the version Flyway has been verified with" 경고가 한 줄 남는 것은 정상입니다. Flyway 11.14는 MySQL 8.0 이상을 지원하며 8.1보다 새 버전에는 경고만 남깁니다.

## 스키마 마이그레이션 (Flyway)

스키마는 `src/main/resources/db/migration/V{n}__{설명}.sql` 로만 변경합니다. 앱이 기동할 때 Flyway가 미적용 버전을 순서대로 실행하고 `flyway_schema_history`에 기록하며, 그다음 Hibernate가 엔티티와 스키마를 검증합니다(`spring.jpa.hibernate.ddl-auto=validate`, 공통 설정). 로컬 H2(`MODE=MySQL`)와 테스트도 같은 파일로 스키마를 만들므로 문법 오류와 엔티티 불일치는 로컬에서 바로 드러납니다. 다만 H2는 MySQL 고유 동작(FK 보조 인덱스 자동 생성, 콜레이션 등)까지 재현하지 않으므로 MySQL 전용 구문은 따로 확인합니다.

- 엔티티를 바꾸면 같은 커밋에 `V{n+1}__*.sql`을 추가합니다. 기동 시 마이그레이션이 검증보다 먼저 실행되므로 컬럼 추가는 순서 문제가 없습니다. 컬럼 삭제는 앱 롤백 시 복구할 수 없으므로 한 배포 뒤로 미룹니다.
- 이미 배포된 버전 파일은 수정하지 않습니다. 체크섬이 달라져 기동에 실패합니다.
- 테스트는 컨텍스트마다 다른 이름의 인메모리 H2에 Flyway로 스키마를 만듭니다(`application-test.properties`). `@DataJpaTest`가 MODE 없는 임베디드 DB로 바꾸지 않도록 `spring.test.database.replace=none`을 둡니다.

**도입 이력**

| 버전 | 내용 |
|------|------|
| V1 | 초기 스키마. 운영 MySQL 8.4 덤프 기준. 운영 DB는 Flyway 이전에 만들어졌으므로 2026-09-17 첫 배포에서 V1을 실행하지 않고 baseline(version 1)으로 등록했습니다(`baseline-on-migrate`, 이후 제거) |
| V2 | 엔티티 없이 운영에만 남아 있던 잔재 테이블 `daily_box_office` 삭제. 이후 운영 스키마와 새 설치 스키마가 동일해졌습니다 |

## 이미지 빌드

멀티스테이지 Dockerfile로 `eclipse-temurin:21-jdk`에서 빌드하고, 최종 이미지는 `eclipse-temurin:21-jre`만 포함합니다. JDK·소스코드·Gradle 캐시가 배포 이미지에 포함되지 않아 이미지 크기를 줄입니다.

```dockerfile
FROM eclipse-temurin:21-jdk AS builder
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:21-jre   # 런타임 이미지만 배포
COPY --from=builder /app/build/libs/*.jar app.jar
```

JVM 옵션은 `JAVA_TOOL_OPTIONS` 환경 변수로 주입합니다. Dockerfile을 수정하지 않고 Compose 설정만으로 힙 크기를 조정할 수 있어, 인스턴스 사양이 바뀌어도 이미지 재빌드가 필요 없습니다.

## 컨테이너 시작 순서

`depends_on` + `healthcheck`로 MySQL과 Redis가 완전히 기동한 뒤에만 Spring Boot 컨테이너가 시작됩니다. `mysqladmin ping`을 10초 간격·최대 10회 재시도해 초기화 중 연결 실패를 방지합니다.
