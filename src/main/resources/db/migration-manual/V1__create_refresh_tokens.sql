-- refresh_tokens 테이블 (MySQL / prod)
--
-- 이 프로젝트는 Flyway/Liquibase가 없고 prod 프로필은 spring.jpa.hibernate.ddl-auto=validate 이므로,
-- 애플리케이션이 스키마를 자동 생성하지 않는다. 토큰 기반 인증 배포 전에 이 DDL을 수동으로 실행해야 한다.
-- (local 프로필은 H2 + ddl-auto=update 라 자동 생성되므로 실행 불필요.)
--
-- 적용:  mysql -u <user> -p <database> < V1__create_refresh_tokens.sql

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    family_id   VARCHAR(36)  NOT NULL,
    issued_at   DATETIME(6)  NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    revoked_at  DATETIME(6)  NULL,
    user_agent  VARCHAR(512) NULL,
    ip_address  VARCHAR(45)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    KEY idx_refresh_token_family (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
