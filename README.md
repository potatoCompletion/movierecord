# MovieRecord

영화·TV 감상 기록 웹 서비스. 본 콘텐츠의 평점, 감정, 스토리, 몰입감 등을 기록하고 마이페이지에서 통계로 확인합니다.

## 기술 스택

- Java 21 / Spring Boot 4.0.5
- Spring MVC + Thymeleaf
- Spring Data JPA / Hibernate
- Spring Security (폼 로그인 + OAuth2 소셜 로그인)
- H2 (로컬) / MySQL 8.4 (운영)
- Docker, Nginx
- Lombok, Bean Validation

## 주요 기능

### 감상 기록

- TMDB 검색으로 영화·TV 작품 선택 (썸네일 자동 연동)
- 감상일, 한줄평, 좋았던 점, 아쉬웠던 점 작성
- 평가 항목 선택
  - **몰입감** (Immersion): 좋음 / 보통 / 나쁨
  - **스토리** (Story): 납득 / 그저 그럼 / 나쁨
  - **감정** (Emotion): 웃김 / 긴장감 / 슬픔 / 여운 / 공포 / 없음
  - **취향 일치도** (Taste): 일치 / 불일치
- 별점 (0.0 ~ 5.0)
- 정렬(최신순·별점순) 및 페이지네이션

### 마이페이지 통계

- 전체 / 올해 / 이번달 기록 수
- 평균 별점
- 취향 일치율
- 월별 감상 기록 그래프 (최근 12개월)
- 감정 분포 도넛 차트

### 인증 / 회원

- 폼 로그인 (아이디·비밀번호)
- OAuth2 소셜 로그인 (Google 등)
- 회원가입 후 관리자 승인 대기 (PENDING → ACTIVE)
- 닉네임 변경 (중복 실시간 확인 포함)

### 관리자

- `/admin/**` 경로, `ROLE_ADMIN` 권한
- 회원 승인 처리

## 패키지 구조

```
com.my.movierecord
├── record/
│   ├── controller/    RecordController
│   ├── service/       WatchRecordService, WatchRecordSaveCommand
│   ├── repository/    WatchRecordRepository
│   ├── domain/        WatchRecord 엔티티
│   ├── enums/         Emotion, Immersion, Story, Taste
│   ├── dto/           RecordForm, RecordListItem, RecordDetail, RecordPageDto, SortOption
│   └── stats/         MyPageStatsService, WatchRecordStatsRepository,
│                      MyPageStats, MonthlyPoint, EmotionSegment, EmotionDistributionCalculator
├── mypage/
│   └── controller/    MyPageController
├── content/
│   ├── domain/        Content (TMDB 콘텐츠 정보 캐시)
│   ├── repository/    ContentRepository
│   └── service/       ContentService
├── tmdb/
│   ├── client/        TmdbClient
│   ├── config/        TmdbProperties
│   ├── controller/    TmdbController
│   └── dto/           TmdbSearchItem
├── auth/
│   ├── controller/    AuthController, AdminController
│   ├── service/       UserService, CustomOAuth2UserService
│   ├── repository/    UserRepository
│   ├── domain/        User 엔티티
│   ├── oauth/         OAuthAttributes, CustomUserPrincipal
│   ├── dto/           SignupForm, NicknameUpdateForm
│   ├── enums/         UserStatus (PENDING, ACTIVE)
│   └── exception/     UserAlreadyExistsException
├── home/
│   └── controller/    HomeController (/ → redirect /contents)
├── common/
│   ├── exception/     GlobalExceptionHandler
│   └── service/       FileStorageService (UUID 파일명 저장)
└── config/
    SecurityConfig, LocalDevSecurityConfig, WebConfig, JpaAuditingConfig,
    TmdbConfig, TimeConfig, PasswordEncoderConfig, DataInitializer, ProdDataInitializer
```

### 주요 설계 포인트

- **프로파일**: `local`(H2, DDL auto-update), `prod`(MySQL, Docker). `application.properties`에 공통 설정.
- **TMDB 연동**: `TmdbClient`가 TMDB API를 호출해 작품 검색. `ContentService`가 첫 기록 시 DB에 캐싱하고 이후에는 재사용.
- **파일 업로드**: `app.upload.dir` 프로퍼티로 경로 지정. `FileStorageService`가 UUID 파일명으로 저장, `WebConfig`가 `/uploads/**`로 노출.
- **JPA Auditing**: `@EnableJpaAuditing`이 `JpaAuditingConfig`에 선언되어 `createdAt`/`updatedAt` 자동 관리.
- **배치 로딩**: `hibernate.default_batch_fetch_size=100` 으로 N+1 방지. `@ElementCollection(emotions)`은 `LAZY`로 선언하여 페이지네이션 시 in-memory 로딩 경고 없이 배치 조회.

## 로컬 개발

```bash
# 실행 (application-local.properties 자동 적용)
./gradlew bootRun

# 컴파일 확인
./gradlew compileJava

# 테스트
./gradlew test
```

- H2 콘솔: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/movierecord;AUTO_SERVER=TRUE`
- 사용자명: `sa` / 비밀번호: 없음

### 필수 환경 변수 (로컬)

`src/main/resources/application-local.properties` 또는 환경 변수로 설정:

```properties
TMDB_API_TOKEN=Bearer eyJ...   # TMDB Read Access Token
```

## 운영 배포 (Docker Compose)

### 1. .env 파일 생성

```env
# Spring
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/{데이터베이스명}?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME={DB 사용자명}
SPRING_DATASOURCE_PASSWORD={DB 비밀번호}

# MySQL
MYSQL_ROOT_PASSWORD={루트 비밀번호}
MYSQL_DATABASE={데이터베이스명}
MYSQL_USER={DB 사용자명}
MYSQL_PASSWORD={DB 비밀번호}

# TMDB
TMDB_API_TOKEN=Bearer eyJ...
```

### 2. 실행

```bash
docker compose up -d
```

- 서비스는 포트 80(Nginx)으로 노출됩니다.
- HTTPS는 `nginx/default.conf`에서 설정하고 포트 443을 활성화합니다.
- 업로드 파일은 `./uploads` 디렉토리에 저장됩니다.
- MySQL 데이터는 Docker 볼륨 `mysql_data`에 유지됩니다.
