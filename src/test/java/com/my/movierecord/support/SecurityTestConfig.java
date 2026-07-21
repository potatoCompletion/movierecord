package com.my.movierecord.support;

import com.my.movierecord.auth.handler.LoginFailureHandler;
import com.my.movierecord.auth.handler.LoginSuccessHandler;
import com.my.movierecord.auth.security.CookieUtil;
import com.my.movierecord.auth.security.JwtProvider;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@code @WebMvcTest} 슬라이스가 {@code @Import(SecurityConfig.class)} 할 때 필요한
 * 인증 관련 빈을 제공한다.
 *
 * <p>{@code SecurityConfig}는 {@code LoginSuccessHandler}/{@code LoginFailureHandler}(둘 다 목),
 * 그리고 토큰 인증 전환으로 새로 추가된 {@code JwtProvider}/{@code CookieUtil}(실제 빈)을 주입받는다.
 * 이들은 {@code @Component}라 웹 슬라이스에 자동 포함되지 않으므로 여기서 등록한다.
 * ({@code @AuthenticationPrincipal} 주입은 {@code SecurityMockMvcRequestPostProcessors.user()}로
 * 이뤄지므로 JWT 필터/토큰과 무관하게 동작한다.)
 */
@TestConfiguration
public class SecurityTestConfig {

    @Bean
    LoginSuccessHandler loginSuccessHandler() {
        return Mockito.mock(LoginSuccessHandler.class);
    }

    @Bean
    LoginFailureHandler loginFailureHandler() {
        return Mockito.mock(LoginFailureHandler.class);
    }

    @Bean
    JwtProvider jwtProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.access-token-ttl-seconds:900}") long accessTtl) {
        return new JwtProvider(secret, accessTtl);
    }

    @Bean
    CookieUtil cookieUtil(@Value("${app.cookie.secure:false}") boolean secure,
                          @Value("${app.jwt.access-token-ttl-seconds:900}") long accessTtl,
                          @Value("${app.jwt.refresh-token-ttl-seconds:1209600}") long refreshTtl) {
        return new CookieUtil(secure, accessTtl, refreshTtl);
    }
}
