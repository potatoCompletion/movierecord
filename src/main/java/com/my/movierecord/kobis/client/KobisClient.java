package com.my.movierecord.kobis.client;

import com.my.movierecord.common.exception.ExternalApiTransientException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import kr.or.kobis.kobisopenapi.consumer.rest.KobisOpenAPIRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * KOBIS 일별 박스오피스 조회를 감싸는 얇은 클라이언트.
 *
 * <p>벤더 JAR({@link KobisOpenAPIRestService})은 자체 HTTP 전송을 캡슐화하고 있어
 * 커넥트/리드 타임아웃이나 HTTP 상태 코드를 노출하지 않는다. 따라서 상태 코드 기반의
 * transient/client 구분이 불가능하며, 호출 자체가 던지는 모든 예외를
 * {@link ExternalApiTransientException}으로 변환해 resilience4j 재시도/서킷브레이커
 * 집계 대상으로 삼는다. 전송 제어가 불가능한 대신 Bulkhead로 동시 점유를 제한한다.
 *
 * <p>박스오피스는 부가 영역이므로 최종 실패 시 {@code null}로 degrade 한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KobisClient {

    private static final String INSTANCE = "kobisApi";

    private final KobisOpenAPIRestService kobisOpenAPIRestService;

    @Bulkhead(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE, fallbackMethod = "fetchFallback")
    public String fetchDailyBoxOffice(String targetDate) {
        try {
            return kobisOpenAPIRestService.getDailyBoxOffice(true, targetDate, "10", "", "", "");
        } catch (Exception e) {
            throw new ExternalApiTransientException("kobis", null,
                    "KOBIS daily box office call failed for " + targetDate, e);
        }
    }

    @SuppressWarnings("unused")
    private String fetchFallback(String targetDate, Throwable t) {
        log.warn("KOBIS box office unavailable for {}: {}", targetDate, t.toString());
        return null;
    }
}
