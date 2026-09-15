package com.my.movierecord.auth.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CookieOAuth2AuthorizationRequestRepositoryTest {

    private static final String SECRET = "test-secret-key-for-jwt-signing-1234567890";
    private static final String COOKIE_PREFIX = CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME + "=";

    private final CookieOAuth2AuthorizationRequestRepository repository =
            new CookieOAuth2AuthorizationRequestRepository(JsonMapper.builder().build(), SECRET, false);

    @Test
    void 인가_요청을_서명된_쿠키에_저장하고_콜백에서_그대로_복원한다() {
        OAuth2AuthorizationRequest original = sampleRequest();
        MockHttpServletResponse redirect = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(original, new MockHttpServletRequest("GET", "/oauth2/authorization/google"), redirect);

        String setCookie = redirect.getHeader("Set-Cookie");
        assertThat(setCookie).startsWith(COOKIE_PREFIX).contains("HttpOnly").contains("SameSite=Lax");
        assertThat(redirect.getHeaders("Set-Cookie")).noneMatch(h -> h.startsWith("JSESSIONID="));

        MockHttpServletRequest callback = callbackWithCookie(cookieValue(setCookie));
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuth2AuthorizationRequest restored = repository.removeAuthorizationRequest(callback, response);

        assertThat(restored).isNotNull();
        assertThat(restored.getState()).isEqualTo(original.getState());
        assertThat(restored.getClientId()).isEqualTo(original.getClientId());
        assertThat(restored.getRedirectUri()).isEqualTo(original.getRedirectUri());
        assertThat(restored.getScopes()).isEqualTo(original.getScopes());
        assertThat(restored.getAttributes()).isEqualTo(original.getAttributes());
        assertThat(restored.getAdditionalParameters()).isEqualTo(original.getAdditionalParameters());
        assertThat(restored.getAuthorizationRequestUri()).isEqualTo(original.getAuthorizationRequestUri());
        assertThat(response.getHeader("Set-Cookie")).startsWith(COOKIE_PREFIX + ";").contains("Max-Age=0");
    }

    @Test
    void 서명이_변조된_쿠키는_무시한다() {
        MockHttpServletResponse redirect = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(sampleRequest(), new MockHttpServletRequest(), redirect);
        String value = cookieValue(redirect.getHeader("Set-Cookie"));
        String tampered = value.substring(0, value.length() - 1) + (value.endsWith("A") ? "B" : "A");

        assertThat(repository.loadAuthorizationRequest(callbackWithCookie(tampered))).isNull();
    }

    @Test
    void 다른_비밀키로_서명된_쿠키는_무시한다() {
        CookieOAuth2AuthorizationRequestRepository other = new CookieOAuth2AuthorizationRequestRepository(
                JsonMapper.builder().build(), "another-secret-key-that-is-long-enough-000", false);
        MockHttpServletResponse redirect = new MockHttpServletResponse();
        other.saveAuthorizationRequest(sampleRequest(), new MockHttpServletRequest(), redirect);

        assertThat(repository.loadAuthorizationRequest(
                callbackWithCookie(cookieValue(redirect.getHeader("Set-Cookie"))))).isNull();
    }

    @Test
    void 쿠키가_없으면_null을_돌려주고_삭제_쿠키도_내리지_않는다() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(repository.removeAuthorizationRequest(new MockHttpServletRequest(), response)).isNull();
        assertThat(response.getHeader("Set-Cookie")).isNull();
    }

    private static OAuth2AuthorizationRequest sampleRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("client-id")
                .redirectUri("https://mu-ra-bel.com/login/oauth2/code/google")
                .scopes(Set.of("openid", "profile", "email"))
                .state("state-123")
                .attributes(Map.of("registration_id", "google", "nonce", "nonce-abc"))
                .additionalParameters(Map.of("nonce", "hashed-nonce"))
                .build();
    }

    private static MockHttpServletRequest callbackWithCookie(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        request.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, value));
        return request;
    }

    private static String cookieValue(String setCookieHeader) {
        return setCookieHeader.substring(COOKIE_PREFIX.length(), setCookieHeader.indexOf(';'));
    }
}
