package com.my.movierecord.auth.service;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.enums.UserStatus;
import com.my.movierecord.auth.oauth.OAuthAttributes;
import com.my.movierecord.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    @Autowired
    public CustomOAuth2UserService(UserRepository userRepository) {
        this(userRepository, new DefaultOAuth2UserService());
    }

    CustomOAuth2UserService(UserRepository userRepository, OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate) {
        this.userRepository = userRepository;
        this.delegate = delegate;
    }

    @Override
    @Transactional(noRollbackFor = OAuth2AuthenticationException.class)
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attrs = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        User user = saveOrUpdate(attrs);

        if (user.getStatus() == UserStatus.PENDING) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_pending", "Account is awaiting admin approval", null));
        }

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(user.getRole())),
                attrs.attributes(),
                attrs.nameAttributeKey()
        );
    }

    private User saveOrUpdate(OAuthAttributes attrs) {
        return userRepository
                .findByProviderAndProviderId(attrs.provider(), attrs.providerId())
                .orElseGet(() -> userRepository.save(createUser(attrs)));
    }

    private User createUser(OAuthAttributes attrs) {
        String username = attrs.provider().toLowerCase() + "_" + attrs.providerId();
        return User.builder()
                .username(username)
                // sentinel value — never matches any real password input;
                // form-login for OAuth accounts is rejected in UserService.loadUserByUsername
                .password("{noop}OAUTH_ACCOUNT_NO_PASSWORD")
                .name(attrs.name() != null ? attrs.name() : "소셜 사용자")
                .status(UserStatus.PENDING)
                .role("ROLE_USER")
                .provider(attrs.provider())
                .providerId(attrs.providerId())
                .build();
    }
}
