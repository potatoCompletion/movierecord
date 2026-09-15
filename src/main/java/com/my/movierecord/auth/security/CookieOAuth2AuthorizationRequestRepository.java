package com.my.movierecord.auth.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2 로그인의 인가 요청(state, nonce 등)을 HTTP 세션 대신 서명된 쿠키에 보관한다.
 *
 * <p>기본 {@code HttpSessionOAuth2AuthorizationRequestRepository}는 소셜 로그인 시작 시 세션을 만들어
 * stateless 설정에서도 {@code JSESSIONID}가 발급됐다. 이 구현은 요청을 JSON → Base64URL 로 직렬화하고
 * HMAC-SHA256 서명을 붙여 쿠키에 담는다. 콜백은 제공자에서 돌아오는 최상위 GET 이므로 {@code SameSite=Lax}
 * 쿠키가 함께 전송된다. 서명이 맞지 않거나 형식이 깨진 쿠키는 저장된 요청이 없는 것으로 취급한다.
 */
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String COOKIE_NAME = "OAUTH2_AUTH_REQUEST";

    private static final Duration MAX_AGE = Duration.ofMinutes(10);
    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE = "Lax";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final char SIGNATURE_SEPARATOR = '.';

    private final ObjectMapper objectMapper;
    private final SecretKeySpec signingKey;
    private final boolean secure;

    public CookieOAuth2AuthorizationRequestRepository(ObjectMapper objectMapper, String secret, boolean secure) {
        this.objectMapper = objectMapper;
        this.signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.secure = secure;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String raw = readCookie(request);
        if (raw == null) {
            return null;
        }
        int separator = raw.lastIndexOf(SIGNATURE_SEPARATOR);
        if (separator < 0) {
            return null;
        }
        String payload = raw.substring(0, separator);
        String signature = raw.substring(separator + 1);
        if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII))) {
            return null;
        }
        try {
            return objectMapper.readValue(Base64.getUrlDecoder().decode(payload), Stored.class).toAuthorizationRequest();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            writeCookie(response, "", Duration.ZERO);
            return;
        }
        byte[] json = objectMapper.writeValueAsBytes(Stored.from(authorizationRequest));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        writeCookie(response, payload + SIGNATURE_SEPARATOR + sign(payload), MAX_AGE);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (readCookie(request) != null) {
            writeCookie(response, "", Duration.ZERO);
        }
        return authorizationRequest;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signingKey);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot sign OAuth2 authorization request cookie", ex);
        }
    }

    private static String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .sameSite(SAME_SITE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** 쿠키에 직렬화되는 인가 요청. 이 앱은 authorization_code 방식만 쓰므로 grant/response type 은 고정이다. */
    public record Stored(String authorizationUri,
                         String clientId,
                         String redirectUri,
                         Set<String> scopes,
                         String state,
                         Map<String, Object> additionalParameters,
                         Map<String, Object> attributes,
                         String authorizationRequestUri) {

        static Stored from(OAuth2AuthorizationRequest request) {
            return new Stored(request.getAuthorizationUri(),
                    request.getClientId(),
                    request.getRedirectUri(),
                    request.getScopes(),
                    request.getState(),
                    request.getAdditionalParameters(),
                    request.getAttributes(),
                    request.getAuthorizationRequestUri());
        }

        OAuth2AuthorizationRequest toAuthorizationRequest() {
            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .scopes(scopes)
                    .state(state)
                    .additionalParameters(additionalParameters)
                    .attributes(attributes)
                    .authorizationRequestUri(authorizationRequestUri)
                    .build();
        }
    }
}
