package com.my.movierecord.common.exception;

/**
 * 재시도할 가치가 있는 외부 API 오류. (5xx, 429, 커넥트/리드 타임아웃 등)
 *
 * <p>resilience4j의 {@code retry-exceptions}/{@code record-exceptions}에 등록되어
 * 자동 재시도 및 서킷브레이커 실패 집계 대상이 된다.
 */
public class ExternalApiTransientException extends ExternalApiException {

    public ExternalApiTransientException(String apiName, Integer status, String message) {
        super(apiName, status, message, null);
    }

    public ExternalApiTransientException(String apiName, Integer status, String message, Throwable cause) {
        super(apiName, status, message, cause);
    }
}
