package com.my.movierecord.common.client;

import com.my.movierecord.common.exception.ExternalApiClientException;
import com.my.movierecord.common.exception.ExternalApiTransientException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

/**
 * 여러 {@link RestClient} 빌더에 공통으로 등록해 HTTP 상태 코드를
 * {@link com.my.movierecord.common.exception.ExternalApiException} 계층으로 변환한다.
 *
 * <ul>
 *   <li>5xx 또는 429 → {@link ExternalApiTransientException} (재시도 대상)</li>
 *   <li>그 외 4xx → {@link ExternalApiClientException} (즉시 실패)</li>
 * </ul>
 *
 * <p>커넥트/리드 타임아웃 등 전송 계층 실패는 상태 코드가 없어 이 핸들러를 타지 않고
 * {@code ResourceAccessException}으로 전파되므로, resilience4j 설정에서 해당 예외를
 * 별도로 재시도 대상에 포함한다.
 */
public final class ExternalApiErrorHandler {

    private static final int TOO_MANY_REQUESTS = 429;

    private ExternalApiErrorHandler() {
    }

    public static RestClient.ResponseSpec.ErrorHandler forApi(String apiName) {
        return (request, response) -> {
            HttpStatusCode statusCode = response.getStatusCode();
            int code = statusCode.value();
            String message = String.format("%s API returned HTTP %d for %s",
                    apiName, code, request.getURI());

            if (statusCode.is5xxServerError() || code == TOO_MANY_REQUESTS) {
                throw new ExternalApiTransientException(apiName, code, message);
            }
            throw new ExternalApiClientException(apiName, code, message);
        };
    }
}
