# MovieRecord

영화 감상 기록 웹 서비스. 본 영화의 평점, 감정, 스토리, 몰입감 등을 기록하고 관리합니다.

## 기술 스택

- Java 21 / Spring Boot 4.0.5
- Spring MVC + Thymeleaf
- Spring Data JPA / Hibernate
- H2 (로컬) / MySQL 8.4 (운영)
- Docker, Nginx

## 로컬 개발

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

`local` 프로파일을 명시해야 `application-local.properties`가 적용됩니다. H2 콘솔은 http://localhost:8080/h2-console 에서 확인할 수 있습니다.

- JDBC URL: `jdbc:h2:file:./data/movierecord;AUTO_SERVER=TRUE`
- 사용자명: `sa` / 비밀번호: 없음

## 운영 배포 (Docker Compose)

### 1. .env 파일 생성

```env
# Spring
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/{데이터베이스명}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
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

서비스는 포트 80(Nginx)으로 노출됩니다. 업로드 파일은 `./uploads` 디렉토리에 저장됩니다.
