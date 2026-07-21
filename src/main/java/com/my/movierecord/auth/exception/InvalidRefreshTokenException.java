package com.my.movierecord.auth.exception;

/** 리프레시 토큰이 존재하지 않거나 이미 폐기(재사용 탐지 포함)되어 인증할 수 없을 때. → 401 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
