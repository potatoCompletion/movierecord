# MovieRecord

영화 감상 기록 웹 서비스. 본 영화의 평점, 감정, 스토리, 몰입감 등을 기록하고 관리합니다.

## 기술 스택

- Java 21 / Spring Boot 4.0.5
- Spring MVC + Thymeleaf
- Spring Data JPA / Hibernate
- Spring Security (폼 로그인 + OAuth2 소셜 로그인)
- H2 (로컬) / MySQL 8.4 (운영)
- Docker, Nginx
- Lombok, Bean Validation

## 주요 기능

### 영화 기록
- 영화 제목, 감상일, 썸네일 이미지 등록
- 한줄평, 좋았던 점, 아쉬웠던 점 작성
- 평가 항목 선택
  - **몰입감** (Immersion): 좋음 / 보통 / 나쁨
  - **스토리** (Story): 납득 / 그저 그럼 / 나쁨
  - **감정** (Emotion): 웃김 / 긴장감 / 슬픔 / 여운 / 없음
  - **취향 일치도** (Taste): 일치 / 불일치
- 별점 (0.0 ~ 5.0)
- 정렬 및 페이지네이션

### 인증 / 회원
- 폼 로그인 (아이디·비밀번호)
- OAuth2 소셜 로그인 (Google 등)
- 회원가입 후 관리자 승인 대기 (PENDING → ACTIVE)
- 마이페이지: 내 기록 조회, 닉네임 변경 (중복 확인 포함)

### 관리자
- `/admin/**` 경로, `ROLE_ADMIN` 권한
- 회원 승인 처리

## 패키지 구조

```
com.my.movierecord
├── movie/
│   ├── controller/   MovieController, MyPageController, HomeController
│   ├── service/      MovieService, MovieSaveCommand(record)
│   ├── repository/   Spring Data JPA
│   ├── domain/       Movie 엔티티
│   ├── enums/        Emotion, Immersion, Story, Taste
│   └── dto/          MovieForm, MovieListItem, MovieDetail, SortOption
├── auth/
│   ├── controller/   AuthController, AdminController
│   ├── service/      UserService, CustomOAuth2UserService
│   ├── repository/   UserRepository
│   ├── domain/       User 엔티티
│   ├── oauth/        OAuthAttributes, CustomUserPrincipal
│   ├── dto/          SignupForm, NicknameUpdateForm
│   └── enums/        UserStatus (PENDING, ACTIVE)
├── common/
│   └── service/      FileStorageService (UUID 파일명 저장)
└── config/
    ├── SecurityConfig, WebConfig
    ├── JpaAuditingConfig
    └── DataInitializer / ProdDataInitializer
```

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
```

### 2. 실행

```bash
docker compose up -d
```

- 서비스는 포트 80(Nginx)으로 노출됩니다.
- HTTPS는 `nginx/default.conf`에서 설정하고 포트 443을 활성화합니다.
- 업로드 파일은 `./uploads` 디렉토리에 저장됩니다.
- MySQL 데이터는 Docker 볼륨 `mysql_data`에 유지됩니다.
