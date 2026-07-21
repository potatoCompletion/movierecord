package com.my.movierecord.config;

import com.my.movierecord.auth.handler.LoginFailureHandler;
import com.my.movierecord.auth.handler.LoginSuccessHandler;
import com.my.movierecord.auth.handler.OAuth2LoginFailureHandler;
import com.my.movierecord.auth.handler.OAuth2LoginSuccessHandler;
import com.my.movierecord.auth.security.CookieUtil;
import com.my.movierecord.auth.security.JwtAuthenticationFilter;
import com.my.movierecord.auth.security.JwtProvider;
import com.my.movierecord.auth.security.RestAuthenticationEntryPoint;
import com.my.movierecord.auth.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * 토큰(JWT 액세스 + 서버 보관 리프레시) 기반 stateless 보안 설정.
 *
 * <p>세션을 생성하지 않고({@link SessionCreationPolicy#STATELESS}), {@link JwtAuthenticationFilter}가
 * 매 요청 {@code ACCESS_TOKEN} 쿠키를 검증해 인증을 재구성한다. CSRF 토큰 저장소는 세션 비의존
 * {@link CookieCsrfTokenRepository}로 전환했다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private LoginSuccessHandler loginSuccessHandler;

    @Autowired
    private LoginFailureHandler loginFailureHandler;

    @Autowired(required = false)
    private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    @Autowired(required = false)
    private OAuth2LoginFailureHandler oauth2LoginFailureHandler;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private CookieUtil cookieUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/auth/login",
                                "/auth/signup",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/webjars/**",
                                "/error",
                                "/search",
                                "/movie/**",
                                "/tv/**",
                                "/person/**",
                                "/api/tmdb/search/unified"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/token/refresh", "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/records/new").authenticated()
                        .requestMatchers(HttpMethod.GET, "/records", "/records/*").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // actuator 는 관리 포트(9090)에서만 실제 노출되고 host 로 publish 하지 않는다.
                        // 관리 컨텍스트가 이 필터체인을 상속하므로 permitAll 로 열어 Prometheus 스크레이프를 허용한다.
                        // 공개 8080 쪽에는 actuator 엔드포인트가 없어 404 이므로 노출 위험이 없다.
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                // 로그아웃은 리프레시 토큰 폐기 + 쿠키 제거가 필요하므로 커스텀 엔드포인트(POST /auth/logout)로 처리한다.
                .logout(logout -> logout.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, cookieUtil),
                        UsernamePasswordAuthenticationFilter.class);

        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/auth/login")
                    .successHandler(oauth2LoginSuccessHandler != null ? oauth2LoginSuccessHandler : loginSuccessHandler)
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                    )
                    .failureHandler(oauth2LoginFailureHandler)
            );
        }

        return http.build();
    }
}
