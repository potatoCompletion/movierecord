package com.my.movierecord.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 로컬 개발 환경용 보안 설정.
 * local 프로필에서만 활성화되며, H2 콘솔 접근을 허용한다.
 * CSRF 보호를 비활성화하고 frame-options을 sameOrigin으로 설정하여
 * H2 콘솔이 iframe으로 로드될 수 있도록 한다.
 */
@Configuration
@Profile("local")
public class LocalDevSecurityConfig {

    /**
     * H2 콘솔(/h2-console/**)에 대한 보안 필터 체인.
     * 높은 우선순위(Order 1)로 먼저 매칭되어 모든 요청을 허용한다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/h2-console/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}
