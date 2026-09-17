# 외부 API 연동

> [← README](../README.md)

TMDB(작품·인물 정보, 포스터), KOBIS(박스오피스), OMDb(다중 평점) 세 API를 사용합니다. 장애 격리 정책은 [resilience.md](resilience.md), 응답 캐싱은 [cache.md](cache.md)에 있습니다.

## 1. OMDb — 다중 평점 배지

**설계 목표**

영화·TV 상세 페이지에서 IMDb, Rotten Tomatoes, Metacritic 세 기관의 평점을 배지 형태로 함께 표시합니다.

**연동**

Spring Framework의 `RestClient`를 `@Qualifier("omdbRestClient")`로 등록하고 `OmdbClient`에 주입했습니다. `imdbId`를 파라미터로 넘기면 `Ratings` 배열을 파싱해 출처별로 CSS 클래스를 계산해 반환합니다.

```java
// OmdbClient.java — 소스 정규화 및 배지 클래스 계산
private static String normalizeSource(String source) {
    return switch (source) {
        case "Internet Movie Database" -> "IMDb";
        default -> source;
    };
}

private static String computeCssClass(String source, String value) {
    return switch (source) {
        case "Rotten Tomatoes" -> {
            int score = Integer.parseInt(value.replace("%", "").trim());
            yield score >= 60 ? "rating-rt-fresh" : "rating-rt-rotten";
        }
        case "Metacritic" -> {
            int score = Integer.parseInt(value.split("/")[0].trim());
            if (score >= 75) yield "rating-mc-green";
            else if (score >= 50) yield "rating-mc-yellow";
            else yield "rating-mc-red";
        }
        default -> "rating-imdb";
    };
}
```

작품 상세 페이지에서는 TMDB 평점을 첫 번째 항목으로 합성한 뒤 OMDb 결과를 이어 붙입니다. 어느 API가 실패해도 나머지 배지는 정상 표시됩니다. 같은 OMDb 평점을 [스포트라이트](features.md#스포트라이트)의 품질 필터에도 사용합니다.

## 2. KOBIS 박스오피스 + TMDB 포스터 합성

**설계 목표**

KOBIS API로 전날 일별 박스오피스 TOP 10을 가져오고, TMDB API로 포스터 이미지를 보완해 하나의 카드 UI로 완성합니다. 홈의 섹션 제목은 데이터 성격에 맞춰 "전일 기준"으로 표기합니다.

**KOBIS 연동**

공식 SDK(`KobisOpenAPIRestService`)는 생성자에 API 키를 직접 받는 구조입니다. 매 요청마다 인스턴스를 생성하는 대신 `@Bean`으로 등록해 Spring DI로 주입받도록 구성했습니다. API 키는 `@ConfigurationProperties`로 바인딩된 `KobisProperties`에서 한 곳에서 관리됩니다.

```java
// KobisConfig.java
@Bean
public KobisOpenAPIRestService kobisOpenAPIRestService(KobisProperties kobisProperties) {
    return new KobisOpenAPIRestService(kobisProperties.key());
}
```

**TMDB 연동**

Spring Framework의 `RestClient`를 `@Qualifier("tmdbRestClient")`로 등록하고 `TmdbClient`에 주입했습니다(`config/TmdbConfig`, connect 3s / read 5s). 박스오피스 제목 매칭 정확도를 높이기 위해 멀티 검색(`/search/multi`)과 영화 전용 검색(`/search/movie`) 엔드포인트를 분리해 상황에 따라 선택적으로 호출합니다. 모든 응답은 `language=ko-KR`로 한국어를 기본값으로 지정했습니다.

**포스터 합성**

KOBIS 응답의 영화 제목을 키로 TMDB 영화 전용 검색을 수행하고, 결과의 `poster_path`를 박스오피스 카드에 채웁니다. 초기에 멀티 검색만 사용했을 때 TV 시리즈 결과가 섞여 포스터가 잘못 매칭되는 문제가 있었습니다. 영화 전용 검색 엔드포인트를 분리해 적용한 뒤 정확도가 개선됐습니다.

## 3. TMDB "곧 개봉해요" — 한국 개봉일 기준 D-Day

**문제**

`discover/movie`에 `region=KR`, `with_release_type=3`(극장 개봉), 개봉일 범위(내일 ~ 21일 후)를 주면 TMDB는 한국 극장 개봉일로 필터링하지만, 응답의 `release_date`에는 전세계 최초 개봉일을 돌려줍니다. 재개봉작이 후보에 들어오면 최초 개봉일이 수년 전이라 `D--2702` 같은 음수 D-Day가 표시됐습니다.

**해결**

- `TmdbHomeService.getUpcoming()`이 discover 후보 상위 8건(popularity 순)마다 `/movie/{id}/release_dates`를 조회해 한국 극장 개봉일을 확정합니다. `TmdbReleaseDates.koreanTheatricalDate()`가 `iso_3166_1 == "KR"` 항목 중 type 3(Theatrical)에서 오늘 이후 가장 이른 날짜를 고르고, 없으면 type 2(Theatrical limited)에 같은 규칙을 적용합니다. 재개봉작은 KR 목록에 과거 원개봉일과 미래 재개봉일이 함께 오므로 "오늘 이후 가장 이른 날짜"를 골라야 합니다.
- 작품별 조회가 실패하면 해당 작품만 discover의 최초 개봉일로 폴백하고 나머지는 정상 처리합니다. 전체를 실패시키지 않습니다.
- 캐시(`upcomingMovies`, 1일)에는 개봉일(ISO 문자열)과 재개봉 여부(최초 개봉일보다 1년 이상 늦으면 `reRelease=true`)만 저장하고, D-Day는 요청 시각 기준으로 `UpcomingCard`가 계산합니다. 캐시가 하루 묵어도 D-Day가 어긋나지 않고, 개봉일이 지난 항목은 `ddays < 0`으로 걸러집니다.
- 템플릿은 `D-DAY` / `D-n`으로 출력하고 재개봉작에는 "재개봉" 뱃지를 붙입니다.

**남은 트레이드오프**

작품별 조회가 부분 실패한 결과도 하루 캐시됩니다. 실패한 작품이 재개봉작이거나 해외 개봉일이 한국보다 앞선 신작이면, 폴백 날짜가 과거라 하루 동안 카드가 사라지거나 D-Day가 실제보다 작게 표시될 수 있습니다. 표시 수준의 문제이고 TTL 후 자연 복구되므로, 부분 실패마다 매 요청 TMDB를 9회 호출하는 대안보다 현 상태를 택했습니다.

## 4. 이미지 서빙 — TMDB CDN 직접 참조

초기에는 TMDB 포스터를 서버 디스크에 내려받아 `/uploads/` 경로로 직접 서빙했습니다. 인스턴스를 교체하면 캐시가 통째로 사라지고, 이미지 트래픽을 EC2가 그대로 부담하며, 파일 저장·삭제·경로 검증 코드를 계속 유지해야 했습니다.

`Content` 엔티티에 TMDB의 `poster_path`만 저장하고 렌더링 시점에 CDN URL을 조립하는 방식으로 전환했습니다. 기존 데이터는 TMDB 상세 API를 호출하는 일회성 백필 엔드포인트로 채운 뒤 엔드포인트를 제거했습니다.

URL 조립은 `TmdbImageUrlProvider` 한 곳으로 모았습니다. 이미지 사이즈를 `PosterSize` enum으로 관리하므로, 향후 CDN을 교체하거나 사이즈 정책을 바꿀 때 이 클래스만 수정하면 됩니다.

```java
// TmdbImageUrlProvider — 포스터·배경·프로필이 같은 조립 규칙을 공유
public String poster(String posterPath, PosterSize size) {
    return build(posterPath, size);
}

private String build(String path, PosterSize size) {
    if (path == null || path.isBlank()) {
        return null;
    }
    return baseUrl + "/" + size.value() + "/" + stripLeadingSlash(path);
}
```

결과적으로 파일 저장 서비스, 이미지 다운로드 클라이언트, 정적 리소스 핸들러, Nginx의 `/uploads/` location, 볼륨 마운트가 모두 제거됐습니다. 브라우저는 서버를 거치지 않고 TMDB CDN에서 직접 이미지를 받습니다.
