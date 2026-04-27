package com.my.movierecord.auth.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthAttributesTest {

    @Test
    @DisplayName("Google 속성으로 OAuthAttributes 생성 성공")
    void ofGoogle_validAttributes_returnsCorrectAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google123");
        attributes.put("name", "홍길동");
        attributes.put("email", "hong@gmail.com");

        OAuthAttributes result = OAuthAttributes.of("google", "sub", attributes);

        assertThat(result.providerId()).isEqualTo("google123");
        assertThat(result.provider()).isEqualTo("GOOGLE");
        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.email()).isEqualTo("hong@gmail.com");
        assertThat(result.nameAttributeKey()).isEqualTo("sub");
    }

    @Test
    @DisplayName("Naver 속성으로 OAuthAttributes 생성 성공")
    void ofNaver_validAttributes_returnsCorrectAttributes() {
        Map<String, Object> response = new HashMap<>();
        response.put("id", "naver456");
        response.put("name", "김철수");
        response.put("email", "kim@naver.com");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("response", response);

        OAuthAttributes result = OAuthAttributes.of("naver", "response", attributes);

        assertThat(result.providerId()).isEqualTo("naver456");
        assertThat(result.provider()).isEqualTo("NAVER");
        assertThat(result.name()).isEqualTo("김철수");
        assertThat(result.email()).isEqualTo("kim@naver.com");
        assertThat(result.nameAttributeKey()).isEqualTo("id");
        assertThat(result.attributes()).containsKey("id");
    }

    @Test
    @DisplayName("Naver response 필드 누락 시 OAuth2AuthenticationException 발생")
    void ofNaver_missingResponseField_throwsException() {
        Map<String, Object> attributes = Map.of("resultcode", "00");

        assertThatThrownBy(() -> OAuthAttributes.of("naver", "response", attributes))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(ex -> assertThat(((OAuth2AuthenticationException) ex).getError().getErrorCode())
                        .isEqualTo("invalid_user_info_response"));
    }

    @Test
    @DisplayName("Naver id 필드 null 시 OAuth2AuthenticationException 발생")
    void ofNaver_nullProviderId_throwsException() {
        Map<String, Object> response = new HashMap<>();
        response.put("id", null);
        response.put("name", "김철수");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("response", response);

        assertThatThrownBy(() -> OAuthAttributes.of("naver", "response", attributes))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(ex -> assertThat(((OAuth2AuthenticationException) ex).getError().getErrorCode())
                        .isEqualTo("missing_provider_id"));
    }

    @Test
    @DisplayName("Google providerId null 시 OAuth2AuthenticationException 발생")
    void ofGoogle_nullProviderId_throwsException() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", null);
        attributes.put("name", "홍길동");

        assertThatThrownBy(() -> OAuthAttributes.of("google", "sub", attributes))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(ex -> assertThat(((OAuth2AuthenticationException) ex).getError().getErrorCode())
                        .isEqualTo("missing_provider_id"));
    }
}
