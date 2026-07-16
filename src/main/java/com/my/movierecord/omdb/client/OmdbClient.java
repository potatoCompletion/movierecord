package com.my.movierecord.omdb.client;

import com.my.movierecord.omdb.config.OmdbProperties;
import com.my.movierecord.omdb.dto.OmdbRating;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OMDB 평점 조회 클라이언트.
 *
 * <p>평점 배지는 부가 영역이므로 resilience4j로 보호하되 최종 실패 시 빈 목록으로
 * degrade 한다. 기존의 광범위 {@code try/catch → List.of()}(무로그)는 제거하고,
 * 실패는 예외로 전파해 resilience4j가 재시도/서킷브레이커 집계에 반영하도록 한다.
 * 최종 fallback에서만 로그를 남긴다.
 */
@Component
@Slf4j
public class OmdbClient {

    private static final String INSTANCE = "omdbApi";

    private final RestClient restClient;
    private final OmdbProperties omdbProperties;

    public OmdbClient(@Qualifier("omdbRestClient") RestClient restClient,
            OmdbProperties omdbProperties) {
        this.restClient = restClient;
        this.omdbProperties = omdbProperties;
    }

    @Bulkhead(name = INSTANCE)
    @RateLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE, fallbackMethod = "getRatingsFallback")
    public List<OmdbRating> getRatings(String imdbId) {
        if (imdbId == null || imdbId.isBlank()) {
            return List.of();
        }
        Map<String, Object> raw = restClient.get()
                .uri(b -> b.queryParam("i", imdbId).queryParam("apikey", omdbProperties.key()).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (raw == null || !"True".equals(raw.get("Response"))) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ratings = (List<Map<String, Object>>) raw.get("Ratings");
        if (ratings == null) {
            return List.of();
        }

        return ratings.stream()
                .map(r -> {
                    String normalized = normalizeSource((String) r.get("Source"));
                    String value = (String) r.get("Value");
                    return new OmdbRating(normalized, value, computeCssClass(normalized, value));
                })
                .toList();
    }

    @SuppressWarnings("unused")
    private List<OmdbRating> getRatingsFallback(String imdbId, Throwable t) {
        log.warn("OMDB ratings fetch failed for {}: {}", imdbId, t.toString());
        return List.of();
    }

    private static String normalizeSource(String source) {
        return switch (source) {
            case "Internet Movie Database" -> "IMDb";
            default -> source;
        };
    }

    private static String computeCssClass(String source, String value) {
        return switch (source) {
            case "IMDb" -> "rating-imdb";
            case "Rotten Tomatoes" -> {
                try {
                    int score = Integer.parseInt(value.replace("%", "").trim());
                    yield score >= 60 ? "rating-rt-fresh" : "rating-rt-rotten";
                } catch (NumberFormatException e) {
                    yield "rating-rt-fresh";
                }
            }
            case "Metacritic" -> {
                try {
                    int score = Integer.parseInt(value.split("/")[0].trim());
                    if (score >= 75) yield "rating-mc-green";
                    else if (score >= 50) yield "rating-mc-yellow";
                    else yield "rating-mc-red";
                } catch (NumberFormatException e) {
                    yield "rating-mc-green";
                }
            }
            default -> "";
        };
    }
}
