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

- **박스오피스 TOP 10**: KOBIS 오픈API로 전날 일별 TOP 10 자동 조회. 포스터(TMDB), 순위, 누적관객수 무한 스크롤 표시
- **감상 기록**: TMDB 검색으로 작품 선택 후 별점·한줄평·몰입감·스토리·감정·취향 일치도 기록
- **마이페이지 통계**: 기록 수, 평균 별점, 취향 일치율, 월별 그래프, 감정 분포 차트
- **인증**: 폼 로그인 + OAuth2 소셜 로그인. 가입 후 관리자 승인(PENDING → ACTIVE)

## 로컬 개발

```bash
./gradlew bootRun   # application-local.properties 자동 적용
./gradlew test
```

H2 콘솔: `http://localhost:8080/h2-console` / JDBC URL: `jdbc:h2:file:./data/movierecord;AUTO_SERVER=TRUE`

### 필수 환경 변수

`src/main/resources/application-local.properties` 또는 환경 변수로 설정:

```properties
TMDB_API_TOKEN=Bearer eyJ...
KOBIS_API_KEY=
```

## 운영 배포

`.env` 파일 작성 후 `docker compose up -d` (포트 80, Nginx 경유)
