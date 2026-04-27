package com.my.movierecord.auth.service;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.enums.UserStatus;
import com.my.movierecord.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    private CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        service = new CustomOAuth2UserService(userRepository, delegate);
    }

    @Test
    @DisplayName("신규 Google 사용자 — PENDING 상태로 저장 후 account_pending 예외 발생")
    void loadUser_newGoogleUser_savedAsPendingAndThrows() {
        OAuth2UserRequest request = buildGoogleRequest();
        OAuth2User oAuth2User = buildOAuth2User(Map.of("sub", "g001", "name", "홍길동", "email", "hong@gmail.com"), "sub");
        when(delegate.loadUser(request)).thenReturn(oAuth2User);
        when(userRepository.findByProviderAndProviderId("GOOGLE", "g001")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(ex -> assertThat(((OAuth2AuthenticationException) ex).getError().getErrorCode())
                        .isEqualTo("account_pending"));

        verify(userRepository).save(argThat(u ->
                "google_g001".equals(u.getUsername())
                        && u.getStatus() == UserStatus.PENDING
                        && "GOOGLE".equals(u.getProvider())
                        && "g001".equals(u.getProviderId())
                        // OAuth role must always be ROLE_USER — never derived from OAuth attributes
                        && "ROLE_USER".equals(u.getRole())
        ));
    }

    @Test
    @DisplayName("기존 활성 사용자 — 저장 없이 OAuth2User 반환, 권한·식별자 확인")
    void loadUser_existingActiveUser_returnsPrincipalWithoutSave() {
        OAuth2UserRequest request = buildGoogleRequest();
        OAuth2User oAuth2User = buildOAuth2User(Map.of("sub", "g002", "name", "김철수"), "sub");
        when(delegate.loadUser(request)).thenReturn(oAuth2User);

        User existing = buildActiveUser("g002");
        when(userRepository.findByProviderAndProviderId("GOOGLE", "g002")).thenReturn(Optional.of(existing));

        OAuth2User result = service.loadUser(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("g002");
        assertThat(result.getAuthorities())
                .anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));
        assertThat(result.getAttributes()).containsKey("sub");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("기존 PENDING 사용자 — account_pending 예외 발생")
    void loadUser_existingPendingUser_throwsAccountPending() {
        OAuth2UserRequest request = buildGoogleRequest();
        OAuth2User oAuth2User = buildOAuth2User(Map.of("sub", "g003", "name", "이영희"), "sub");
        when(delegate.loadUser(request)).thenReturn(oAuth2User);

        User pending = buildPendingUser("g003");
        when(userRepository.findByProviderAndProviderId("GOOGLE", "g003")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(ex -> assertThat(((OAuth2AuthenticationException) ex).getError().getErrorCode())
                        .isEqualTo("account_pending"));

        verify(userRepository, never()).save(any());
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private OAuth2UserRequest buildGoogleRequest() {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId("google")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();

        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "token", Instant.now(), Instant.now().plusSeconds(3600));
        return new OAuth2UserRequest(registration, token);
    }

    private OAuth2User buildOAuth2User(Map<String, Object> rawAttributes, String nameAttributeKey) {
        Map<String, Object> attrs = new HashMap<>(rawAttributes);
        return new DefaultOAuth2User(Set.of(), attrs, nameAttributeKey);
    }

    private User buildActiveUser(String providerId) {
        return User.builder()
                .username("google_" + providerId)
                .password("{noop}OAUTH_ACCOUNT_NO_PASSWORD")
                .name("테스트")
                .status(UserStatus.ACTIVE)
                .role("ROLE_USER")
                .provider("GOOGLE")
                .providerId(providerId)
                .build();
    }

    private User buildPendingUser(String providerId) {
        return User.builder()
                .username("google_" + providerId)
                .password("{noop}OAUTH_ACCOUNT_NO_PASSWORD")
                .name("테스트")
                .status(UserStatus.PENDING)
                .role("ROLE_USER")
                .provider("GOOGLE")
                .providerId(providerId)
                .build();
    }
}
