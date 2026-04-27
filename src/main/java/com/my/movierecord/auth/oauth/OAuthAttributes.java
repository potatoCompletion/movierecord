package com.my.movierecord.auth.oauth;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

public record OAuthAttributes(
        String providerId,
        String provider,
        String name,
        String email,
        Map<String, Object> attributes,
        String nameAttributeKey
) {

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> ofGoogle(userNameAttributeName, attributes);
            case "naver"  -> ofNaver(attributes);
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider", "Unknown provider: " + registrationId, null));
        };
    }

    private static OAuthAttributes ofNaver(Map<String, Object> attributes) {
        Object raw = attributes.get("response");
        if (!(raw instanceof Map<?, ?> rawResponse)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info_response"));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) rawResponse;
        String providerId = (String) response.get("id");
        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_provider_id"));
        }
        return new OAuthAttributes(providerId, "NAVER",
                (String) response.get("name"),
                (String) response.get("email"),
                response, "id");
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        String providerId = (String) attributes.get(userNameAttributeName);
        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_provider_id"));
        }
        return new OAuthAttributes(providerId, "GOOGLE",
                (String) attributes.get("name"),
                (String) attributes.get("email"),
                attributes, userNameAttributeName);
    }
}
