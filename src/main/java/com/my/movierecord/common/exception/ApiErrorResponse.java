package com.my.movierecord.common.exception;

import org.springframework.http.HttpStatus;

/**
 * REST 경로의 일관된 에러 응답 본문. 기존의 수기 {@code String.format} JSON을 대체한다.
 */
public record ApiErrorResponse(int status, String error, String message, String path) {

    public static ApiErrorResponse of(HttpStatus status, String message, String path) {
        return new ApiErrorResponse(status.value(), status.getReasonPhrase(), message, path);
    }
}
