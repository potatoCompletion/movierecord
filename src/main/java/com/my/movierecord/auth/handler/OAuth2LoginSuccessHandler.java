package com.my.movierecord.auth.handler;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.dto.TokenPair;
import com.my.movierecord.auth.oauth.CustomUserPrincipal;
import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.auth.security.CookieUtil;
import com.my.movierecord.auth.service.TokenService;
import com.my.movierecord.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 처리. 폼 로그인과 동일하게 토큰을 발급해 쿠키로 내려준다.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final CookieUtil cookieUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String username = ((CustomUserPrincipal) authentication.getPrincipal()).getUsername();
        userService.recordLogin(username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("OAuth2 로그인 사용자를 찾을 수 없습니다: " + username));
        TokenPair tokens = tokenService.issueTokenPair(user);
        cookieUtil.writeTokens(response, tokens);
        response.sendRedirect("/records");
    }
}
