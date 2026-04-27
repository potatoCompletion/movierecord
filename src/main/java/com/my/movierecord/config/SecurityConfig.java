package com.my.movierecord.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 애플리케이션 전역 보안 설정.
 * 인증이 필요한 페이지와 공개 페이지를 구분하고,
 * 폼 로그인, 로그아웃, HTTP 헤더 보안을 설정한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 전역 보안 필터 체인을 구성한다.
     * - 인증 없이 접근 가능한 경로: /auth/**, /css/**, /js/**, /images/**, /uploads/**, /error
     * - 그 외 모든 경로는 인증 필요
     * - 로그인 페이지: /auth/login
     * - 로그인 성공 후 리다이렉트: /movies
     * - 로그아웃 후 리다이렉트: /auth/login?logout
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 공개 페이지 설정
                        .requestMatchers(
                                "/auth/login",
                                "/auth/signup",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/error"
                        ).permitAll()
                        // 나머지는 모두 인증 필요
                        .anyRequest().authenticated()
                )
                // 폼 로그인 설정
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/movies", true)
                        .permitAll()
                )
                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutSuccessUrl("/auth/login?logout")
                        .permitAll()
                )
                // HTTP 헤더 보안 (clickjacking 공격 방지)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}
