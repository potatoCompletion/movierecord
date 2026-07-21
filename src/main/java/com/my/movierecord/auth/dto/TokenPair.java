package com.my.movierecord.auth.dto;

/** 로그인/회전 시 발급되는 액세스 토큰(JWT)과 리프레시 토큰(원문) 쌍. */
public record TokenPair(String accessToken, String refreshToken) {
}
