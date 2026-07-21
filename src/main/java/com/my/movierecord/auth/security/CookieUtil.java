package com.my.movierecord.auth.security;

import com.my.movierecord.auth.dto.TokenPair;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 인증 쿠키(ACCESS_TOKEN/REFRESH_TOKEN) 생성·삭제·조회 헬퍼.
 *
 * <p>둘 다 {@code HttpOnly}. {@code Secure}는 prod(https)에서만 true. OAuth2 외부 IdP 리다이렉트
 * 이후에도 쿠키가 설정되도록 {@code SameSite=Lax}를 쓴다. 리프레시 쿠키는 노출 최소화를 위해
 * {@code Path=/auth}로 스코프를 좁혀 refresh/logout 요청에만 전송된다.
 */
@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";

    private static final String ACCESS_PATH = "/";
    private static final String REFRESH_PATH = "/auth";
    private static final String SAME_SITE = "Lax";

    private final boolean secure;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public CookieUtil(@Value("${app.cookie.secure:false}") boolean secure,
                      @Value("${app.jwt.access-token-ttl-seconds:900}") long accessTtlSeconds,
                      @Value("${app.jwt.refresh-token-ttl-seconds:1209600}") long refreshTtlSeconds) {
        this.secure = secure;
        this.accessTtl = Duration.ofSeconds(accessTtlSeconds);
        this.refreshTtl = Duration.ofSeconds(refreshTtlSeconds);
    }

    public void writeTokens(HttpServletResponse response, TokenPair tokens) {
        addCookie(response, ACCESS_TOKEN, tokens.accessToken(), ACCESS_PATH, accessTtl);
        addCookie(response, REFRESH_TOKEN, tokens.refreshToken(), REFRESH_PATH, refreshTtl);
    }

    public void clearTokens(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN, "", ACCESS_PATH, Duration.ZERO);
        addCookie(response, REFRESH_TOKEN, "", REFRESH_PATH, Duration.ZERO);
    }

    public Optional<String> readAccessToken(HttpServletRequest request) {
        return readCookie(request, ACCESS_TOKEN);
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        return readCookie(request, REFRESH_TOKEN);
    }

    private void addCookie(HttpServletResponse response, String name, String value, String path, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(maxAge)
                .sameSite(SAME_SITE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
