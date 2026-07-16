package com.my.movierecord.common.exception;

/**
 * 재시도해도 성공하기 어려운 외부 API 오류. (400번대, 단 429 제외)
 *
 * <p>resilience4j의 {@code ignore-exceptions}에 등록되어 재시도 대상에서 제외되고
 * 서킷브레이커 실패로도 집계되지 않는다. (잘못된 요청을 반복해봐야 의미 없으므로)
 */
public class ExternalApiClientException extends ExternalApiException {

    public ExternalApiClientException(String apiName, Integer status, String message) {
        super(apiName, status, message, null);
    }

    public ExternalApiClientException(String apiName, Integer status, String message, Throwable cause) {
        super(apiName, status, message, cause);
    }
}
