package com.my.movierecord.auth.exception;

/** 리프레시 토큰의 만료 기한이 지났을 때. → 401 */
public class ExpiredRefreshTokenException extends RuntimeException {
    public ExpiredRefreshTokenException(String message) {
        super(message);
    }
}
