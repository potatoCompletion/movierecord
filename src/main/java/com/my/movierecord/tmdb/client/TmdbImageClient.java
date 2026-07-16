package com.my.movierecord.tmdb.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * TMDB 이미지 호스트(image.tmdb.org)에서 포스터를 내려받는 전용 클라이언트.
 *
 * <p>기존에는 {@code ContentService}의 private 메서드였으나, resilience4j는 Spring AOP
 * 프록시 기반이라 self-invocation에는 적용되지 않는다. 별도 빈으로 분리해 애노테이션이
 * 실제로 동작하도록 한다. 포스터 다운로드는 부가 기능이므로 최종 실패 시 {@code null}로
 * graceful degrade 한다. (포스터 없이 콘텐츠 저장)
 */
@Component
@Slf4j
public class TmdbImageClient {

    private static final String INSTANCE = "tmdbImage";

    private final RestClient restClient;

    public TmdbImageClient(@Qualifier("tmdbImageRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Bulkhead(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE, fallbackMethod = "downloadFallback")
    public byte[] download(String posterPath) {
        return restClient.get()
                .uri(posterPath)
                .retrieve()
                .body(byte[].class);
    }

    @SuppressWarnings("unused")
    private byte[] downloadFallback(String posterPath, Throwable t) {
        log.warn("TMDB image download failed for {}: {}", posterPath, t.toString());
        return null;
    }
}
