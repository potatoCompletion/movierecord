# 수동 DB 마이그레이션

이 프로젝트에는 Flyway/Liquibase가 없다. `prod` 프로필은 `spring.jpa.hibernate.ddl-auto=validate`로
동작하므로 애플리케이션이 스키마를 자동 생성/변경하지 않는다. 새 테이블이 필요한 변경은 여기에 SQL로
남기고, **운영 배포 전에 DBA/운영자가 수동으로 적용**해야 한다.

`local` 프로필은 H2 + `ddl-auto=update`라서 아래 스크립트 없이도 엔티티로부터 자동 생성된다.

## 적용 순서

| 스크립트 | 설명 | 도입 시점 |
|---|---|---|
| `V1__create_refresh_tokens.sql` | 토큰 기반 인증용 `refresh_tokens` 테이블 | 세션→토큰 인증 전환 |

```bash
mysql -u <user> -p <database> < V1__create_refresh_tokens.sql
```

## 후속 과제

Flyway 도입 시 이 디렉터리의 스크립트를 `src/main/resources/db/migration`으로 옮기고 자동화한다.
