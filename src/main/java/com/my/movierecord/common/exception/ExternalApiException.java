package com.my.movierecord.common.exception;

/**
 * 외부 API(TMDB / OMDB / KOBIS) 호출 실패를 표현하는 최상위 예외.
 *
 * <p>resilience4j는 "던져진 예외"로만 실패를 인지하므로, 외부 API 호출 seam에서는
 * 예외를 삼키지 말고 이 계층의 예외로 변환해 던져야 한다. 재시도 가치 여부에 따라
 * {@link ExternalApiTransientException}(재시도 대상) 또는
 * {@link ExternalApiClientException}(즉시 실패)으로 구분한다.
 */
public abstract class ExternalApiException extends RuntimeException {

    private final String apiName;
    private final Integer status;

    protected ExternalApiException(String apiName, Integer status, String message, Throwable cause) {
        super(message, cause);
        this.apiName = apiName;
        this.status = status;
    }

    /** 호출 대상 API 식별자 (예: tmdb, omdb, kobis). */
    public String getApiName() {
        return apiName;
    }

    /** HTTP 상태 코드. 전송 계층 실패 등 상태 코드가 없는 경우 {@code null}. */
    public Integer getStatus() {
        return status;
    }
}
