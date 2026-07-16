package com.my.movierecord.tmdb.client;

import com.my.movierecord.tmdb.dto.NowPlayingItem;
import com.my.movierecord.tmdb.dto.TmdbDiscoverItem;
import com.my.movierecord.tmdb.dto.TmdbDiscoverResponse;
import com.my.movierecord.tmdb.dto.TmdbMovieDetail;
import com.my.movierecord.tmdb.dto.TmdbPersonDetail;
import com.my.movierecord.tmdb.dto.TmdbSearchItem;
import com.my.movierecord.tmdb.dto.TmdbTvDetail;
import com.my.movierecord.tmdb.dto.UpcomingItem;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * TMDB REST API 클라이언트.
 *
 * <p>모든 public 호출은 resilience4j(bulkhead/ratelimiter/circuitbreaker/retry) 로 보호된다.
 * 하이브리드 degradation 정책에 따라 메서드를 두 부류로 나눈다.
 * <ul>
 *   <li><b>핵심 콘텐츠</b>(검색 결과, 영화/TV/인물 상세): fallback 없음 → 예외를 전파해
 *       상위에서 503 에러 페이지로 표면화한다.</li>
 *   <li><b>부가 영역</b>(자동완성, 홈 캐러셀, 박스오피스 보강, 외부 ID): fallbackMethod로
 *       빈 결과/null 을 반환해 조용히 degrade 한다.</li>
 * </ul>
 */
@Component
@Slf4j
public class TmdbClient {

    private static final String INSTANCE = "tmdbApi";

    private static final String SEARCH_MULTI_PATH = "/search/multi";
    private static final String SEARCH_MOVIE_PATH = "/search/movie";
    private static final String MOVIE_PATH = "/movie/{id}";
    private static final String TV_PATH = "/tv/{id}";
    private static final String PERSON_PATH = "/person/{id}";
    private static final String PERSON_CREDITS_PATH = "/person/{id}/combined_credits";
    private static final String TV_EXTERNAL_IDS_PATH = "/tv/{id}/external_ids";
    private static final String NOW_PLAYING_PATH    = "/movie/now_playing";
    private static final String DISCOVER_MOVIE_PATH = "/discover/movie";

    private final RestClient restClient;

    public TmdbClient(@Qualifier("tmdbRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    // --- 부가 영역: 자동완성 검색 (실패 시 빈 목록) ---
    @Bulkhead(name = INSTANCE)
    @RateLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE, fallbackMethod = "searchMultiFallback")
    public List<TmdbSearchItem> searchMulti(String query) {
        Map<String, Object> body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_MULTI_PATH)
                        .queryParam("query", query)
                        .queryParam("include_adult", false)
                        .queryParam("language", "ko-KR")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (body == null) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        if (results == null) {
            return List.of();
        }

        return results.stream()
                .filter(r -> !"person".equals(r.get("media_type")))
                .map(this::toMultiSearchItem)
                .toList();
    }

    @SuppressWarnings("unused")
    private List<TmdbSearchItem> searchMultiFallback(String query, Throwable t) {
        log.warn("TMDB searchMulti failed for '{}': {}", query, t.toString());
        return List.of();
    }

    private TmdbSearchItem toMultiSearchItem(Map<String, Object> raw) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        String mediaType = (String) raw.get("media_type");
        boolean isTv = "tv".equals(mediaType);
        String title = isTv ? (String) raw.get("name") : (String) raw.get("title");
        String posterPath = (String) raw.get("poster_path");
        String releaseDate = isTv ? (String) raw.get("first_air_date") : (String) raw.get("release_date");
        return new TmdbSearchItem(id, title, posterPath, mediaType, releaseDate);
    }

    // --- 부가 영역: KOBIS 박스오피스 보강용 영화 검색 (실패 시 빈 목록) ---
    @Bulkhead(name = INSTANCE)
    @RateLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE, fallbackMethod = "searchMovieFallback")
    public List<TmdbSearchItem> searchMovie(String query, String year) {
        Map<String, Object> body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_MOVIE_PATH)
                        .queryParam("query", query)
                        .queryParam("language", "ko-KR")
                        .queryParam("year", year)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (body == null) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        if (results == null) {
            return List.of();
        }

        return results.stream()
                .map(this::toMovieSearchItem)
                .toList();
    }

    @SuppressWarnings("unused")
    private List<TmdbSearchItem> searchMovieFallback(String query, String year, Throwable t) {
        log.warn("TMDB searchMovie failed for '{}' ({}): {}", query, year, t.toString());
        return List.of();
    }

    private TmdbSearchItem toMovieSearchItem(Map<String, Object> raw) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        String title = (String) raw.get("title");
        String posterPath = (String) raw.get("poster_path");
        String releaseDate = (String) raw.get("release_date");
        return new TmdbSearchItem(id, title, posterPath, "movie", releaseDate);
    }

    // --- 핵심 콘텐츠: 통합 검색 결과 (실패 시 예외 전파 → 503) ---
    @Bulkhead(name = INSTANCE)
    @RateLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public List<TmdbSearchItem> searchMultiUnified(String query) {
        Map<String, Object> body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_MULTI_PATH)
                        .queryParam("query", query)
                        .queryParam("include_adult", false)
                        .queryParam("language", "ko-KR")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (body == null) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        if (results == null) {
            return List.of();
        }

        return results.stream()
                .map(this::toUnifiedSearchItem)
                .toList();
    }

    private TmdbSearchItem toUnifiedSearchItem(Map<String, Object> raw) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        String mediaType = (String) raw.get("media_type");
        boolean isPerson = "person".equals(mediaType);
        boolean isTv = "tv".equals(mediaType);
        String title = (isPerson || isTv) ? (String) raw.get("name") : (String) raw.get("title");
        String posterPath = isPerson ? (String) raw.get("profile_path") : (String) raw.get("poster_path");
        String releaseDate = isPerson ? null
                : (isTv ? (String) raw.get("first_air_date") : (String) raw.get("release_date"));
        return new TmdbSearchItem(id, title, posterPath, mediaType, releaseDate);
    }

    // --- 핵심 콘텐츠: 영화 상세 (실패 시 예외 전파 → 503) ---
    @Bulkhead(name = INSTANCE)
    @RateLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public TmdbMovieDetail getMovieDetail(Long tmdbId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(MOVIE_PATH)
                        .queryParam("language", "ko-KR")
                        .build(tmdbId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return raw != null ? TmdbMovieDetail.from(raw) : null;
    }

    // --- 핵심 콘텐츠: TV 상세 (실패 시 예외 전파 → 503) ---
    @Bulkhead(name = INSTANCE)
    @RateLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public TmdbTvDetail getTvDetail(Long tmdbId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(TV_PATH)
                        .queryParam("language", "ko-KR")
                        .build(tmdbId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return raw != null ? TmdbTvDetail.from(raw) : null;
    }

    // --- 부가 영역: TV 외부 ID(IMDb) 조회 — 평점 배지용 (실패 시 null) ---
    @Bulkhead(name = INSTANCE)
    @RateLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE, fallbackMethod = "getTvExternalIdsFallback")
    public String getTvExternalIds(Long tmdbId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(TV_EXTERNAL_IDS_PATH)
                        .build(tmdbId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return raw != null ? (String) raw.get("imdb_id") : null;
    }

    @SuppressWarnings("unused")
    private String getTvExternalIdsFallback(Long tmdbId, Throwable t) {
        log.warn("TMDB external ids fetch failed for tv {}: {}", tmdbId, t.toString());
        return null;
    }

    // --- 부가 영역: 홈 '현재 상영작' 캐러셀 (실패 시 빈 목록) ---
    @Bulkhead(name = INSTANCE)
    @RateLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE, fallbackMethod = "getNowPlayingFallback")
    @SuppressWarnings("unchecked")
    public List<NowPlayingItem> getNowPlaying() {
        Map<String, Object> body = restClient.get()
                .uri(b -> b.path(NOW_PLAYING_PATH)
                        .queryParam("language", "ko-KR")
                        .queryParam("page", "1")
                        .queryParam("region", "KR")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (body == null) return List.of();
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        return results == null ? List.of() :
                results.stream().map(NowPlayingItem::from).limit(10).toList();
    }

    @SuppressWarnings("unused")
    private List<NowPlayingItem> getNowPlayingFallback(Throwable t) {
        log.warn("TMDB now playing fetch failed: {}", t.toString());
        return List.of();
    }

    // --- 부가 영역: 홈 '곧 개봉해요' 캐러셀 (실패 시 빈 목록) ---
    @Bulkhead(name = INSTANCE)
    @RateLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE, fallbackMethod = "getUpcomingFallback")
    @SuppressWarnings("unchecked")
    public List<UpcomingItem> getUpcoming() {
        LocalDate today = LocalDate.now();
        Map<String, Object> body = restClient.get()
                .uri(b -> b.path(DISCOVER_MOVIE_PATH)
                        .queryParam("include_adult", "false")
                        .queryParam("include_video", "false")
                        .queryParam("language", "ko-KR")
                        .queryParam("page", "1")
                        .queryParam("region", "KR")
                        .queryParam("release_date.gte", today.plusDays(1).toString())
                        .queryParam("release_date.lte", today.plusDays(21).toString())
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("with_release_type", "3")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (body == null) return List.of();
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        if (results == null) return List.of();

        return results.stream()
                .filter(r -> r.get("release_date") instanceof String s && !s.isBlank())
                .map(r -> UpcomingItem.from(r, today))
                .sorted(Comparator.comparingLong(UpcomingItem::ddays))
                .limit(8)
                .toList();
    }

    @SuppressWarnings("unused")
    private List<UpcomingItem> getUpcomingFallback(Throwable t) {
        log.warn("TMDB upcoming fetch failed: {}", t.toString());
        return List.of();
    }

    // --- 부가 영역: 스포트라이트 후보 발굴 (실패 시 빈 응답) ---
    @Bulkhead(name = INSTANCE)
    @RateLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE, fallbackMethod = "discoverHighRatedFallback")
    @SuppressWarnings("unchecked")
    public TmdbDiscoverResponse discoverHighRated(int page) {
        Map<String, Object> body = restClient.get()
                .uri(b -> b.path(DISCOVER_MOVIE_PATH)
                        .queryParam("include_adult", "false")
                        .queryParam("include_video", "false")
                        .queryParam("language", "ko-KR")
                        .queryParam("page", String.valueOf(page))
                        .queryParam("sort_by", "vote_average.desc")
                        .queryParam("vote_average.gte", "7.5")
                        .queryParam("vote_count.gte", "500")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (body == null) return new TmdbDiscoverResponse(List.of(), 1);
        int totalPages = body.get("total_pages") instanceof Number n ? n.intValue() : 1;
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        List<TmdbDiscoverItem> items = results == null ? List.of()
                : results.stream().map(TmdbDiscoverItem::from).toList();
        return new TmdbDiscoverResponse(items, totalPages);
    }

    @SuppressWarnings("unused")
    private TmdbDiscoverResponse discoverHighRatedFallback(int page, Throwable t) {
        log.warn("TMDB discover high-rated failed for page {}: {}", page, t.toString());
        return new TmdbDiscoverResponse(List.of(), 1);
    }

    // --- 핵심 콘텐츠: 인물 상세 (실패 시 예외 전파 → 503) ---
    @Bulkhead(name = INSTANCE)
    @RateLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public TmdbPersonDetail getPersonDetail(Long tmdbId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> personRaw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PERSON_PATH)
                        .queryParam("language", "ko-KR")
                        .build(tmdbId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        @SuppressWarnings("unchecked")
        Map<String, Object> creditsRaw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PERSON_CREDITS_PATH)
                        .queryParam("language", "ko-KR")
                        .build(tmdbId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return personRaw != null ? TmdbPersonDetail.from(personRaw, creditsRaw) : null;
    }
}
