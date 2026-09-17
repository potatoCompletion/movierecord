-- V1: 초기 스키마.
-- 2026-09-17 운영 MySQL 8.4 스키마 덤프(mysqldump --no-data) 기준으로 작성했다.
-- 운영 DB는 baseline(version 1)으로 등록되어 이 파일을 실행하지 않는다. 로컬 H2(MODE=MySQL)와 테스트, 새 MySQL 인스턴스에서만 실행된다.
-- 덤프 대비 변경: AUTO_INCREMENT 시작값 제거, 컬럼별 CHARACTER SET/COLLATE 는 테이블 옵션으로 통일,
-- 엔티티가 없는 잔재 테이블 daily_box_office 는 제외(커밋 6cc4e4c 에서 DB 저장을 Spring Cache 로 전환).
-- H2 는 MODE=MySQL 에서만 이 문법(DATETIME, AUTO_INCREMENT, 테이블 옵션)을 받아들인다.
-- 주의: MySQL 은 인라인 FOREIGN KEY 의 보조 인덱스를 제약 이름으로 자동 생성한다. 같은 이름의 CREATE INDEX 가 뒤따르면
-- 중복 키 이름 오류가 나므로, 인덱스를 먼저 만들고 FK 는 ALTER TABLE 로 붙인다(H2 는 이름을 다르게 붙여 이 차이를 재현하지 못한다).
-- 운영에는 이 파일에 없는 daily_box_office 테이블이 남아 있다(판단 필요 항목 P). 삭제 시에는 DROP TABLE IF EXISTS 로 작성한다.

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password      VARCHAR(255) NOT NULL,
    full_name     VARCHAR(50)  NOT NULL,
    nickname      VARCHAR(50)  NULL,
    status        VARCHAR(10)  NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    provider      VARCHAR(10)  NULL,
    provider_id   VARCHAR(255) NULL,
    created_at    DATETIME(6)  NOT NULL,
    last_login_at DATETIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_nickname UNIQUE (nickname),
    CONSTRAINT uk_user_provider  UNIQUE (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(64)  NOT NULL,
    family_id  VARCHAR(36)  NOT NULL,
    issued_at  DATETIME(6)  NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    revoked_at DATETIME(6)  NULL,
    user_agent VARCHAR(512) NULL,
    ip_address VARCHAR(45)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci; -- 운영 실제 콜레이션(다른 테이블과 다름)을 그대로 반영
CREATE INDEX idx_refresh_token_family ON refresh_tokens (family_id);

CREATE TABLE content (
    tmdb_id     BIGINT       NOT NULL,
    media_type  VARCHAR(20)  NOT NULL,
    poster_path VARCHAR(255) NULL,
    PRIMARY KEY (tmdb_id, media_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE watch_record (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    title              VARCHAR(200) NOT NULL,
    watched_date       DATE         NOT NULL,
    one_liner          TEXT         NULL,
    immersion          VARCHAR(20)  NOT NULL,
    story              VARCHAR(20)  NOT NULL,
    good_points        TEXT         NULL,
    bad_points         TEXT         NULL,
    taste              VARCHAR(20)  NOT NULL,
    rating             DECIMAL(2,1) NOT NULL,
    user_id            BIGINT       NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    content_tmdb_id    BIGINT       NULL,
    content_media_type VARCHAR(20)  NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_watch_record_user_id           ON watch_record (user_id);
CREATE INDEX idx_watch_record_user_watched_date ON watch_record (user_id, watched_date DESC);
CREATE INDEX fk_watch_record_content            ON watch_record (content_tmdb_id, content_media_type);
ALTER TABLE watch_record ADD CONSTRAINT fk_watch_record_user    FOREIGN KEY (user_id) REFERENCES users (id) ON UPDATE CASCADE;
ALTER TABLE watch_record ADD CONSTRAINT fk_watch_record_content FOREIGN KEY (content_tmdb_id, content_media_type) REFERENCES content (tmdb_id, media_type);

CREATE TABLE watch_record_emotion (
    watch_record_id BIGINT      NOT NULL,
    emotion         VARCHAR(20) NOT NULL,
    PRIMARY KEY (watch_record_id, emotion),
    CONSTRAINT fk_watch_record_emotion FOREIGN KEY (watch_record_id) REFERENCES watch_record (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE spotlight_history (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tmdb_id        BIGINT       NOT NULL,
    imdb_id        VARCHAR(20)  NULL,
    title          VARCHAR(200) NOT NULL,
    original_title VARCHAR(200) NULL,
    poster_path    VARCHAR(500) NULL,
    backdrop_path  VARCHAR(500) NULL,
    release_year   VARCHAR(4)   NULL,
    overview       TEXT         NULL,
    tmdb_rating    DOUBLE       NULL,
    rt_score       VARCHAR(10)  NULL,
    selected_at    DATE         NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
