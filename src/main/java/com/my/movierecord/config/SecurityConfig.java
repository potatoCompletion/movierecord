package com.my.movierecord.config;

import com.my.movierecord.auth.handler.LoginFailureHandler;
import com.my.movierecord.auth.handler.LoginSuccessHandler;
import com.my.movierecord.auth.handler.OAuth2LoginFailureHandler;
import com.my.movierecord.auth.handler.OAuth2LoginSuccessHandler;
import com.my.movierecord.auth.security.CookieOAuth2AuthorizationRequestRepository;
import com.my.movierecord.auth.security.CookieUtil;
import com.my.movierecord.auth.security.JwtAuthenticationFilter;
import com.my.movierecord.auth.security.JwtProvider;
import com.my.movierecord.auth.security.RestAuthenticationEntryPoint;
import com.my.movierecord.auth.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * 토큰(JWT 액세스 + 서버 보관 리프레시) 기반 stateless 보안 설정.
 *
 * <p>세션을 생성하지 않고({@link SessionCreationPolicy#STATELESS}), {@link JwtAuthenticationFilter}가
 * 매 요청 {@code ACCESS_TOKEN} 쿠키를 검증해 인증을 재구성한다. CSRF 토큰 저장소는 세션 비의존
 * {@link CookieCsrfTokenRepository}로 전환했다.
 *
 * <p>세션이 전혀 만들어지지 않도록 OAuth2 인가 요청 저장소도 쿠키 기반
 * ({@link CookieOAuth2AuthorizationRequestRepository})을 쓴다. 리다이렉트 flash 속성은 {@code WebConfig}의
 * 쿠키 기반 FlashMapManager 가 담당한다.
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

    /**
     * OAuth2 로그인 인가 요청(state/nonce)을 세션 대신 서명된 쿠키에 보관한다.
     * 기본 HttpSession 저장소는 소셜 로그인 시작 시 JSESSIONID 를 발급하므로 교체한다.
     */
    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.cookie.secure:false}") boolean secure) {
        return new CookieOAuth2AuthorizationRequestRepository(objectMapper, secret, secure);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository)
            throws Exception {
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
                        // 관리 컨텍스트가 이 필터체인을 상속하므로 permitAll 로 열어 내부 네트워크에서 접근할 수 있게 한다.
                        // 공개 8080 쪽에는 actuator 엔드포인트가 없어 404 이므로 노출 위험이 없다.
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // 기본 CsrfAuthenticationStrategy 는 "인증 시" 기존 XSRF-TOKEN 쿠키를 삭제하고
                        // 새 토큰은 지연 생성으로 남긴다. 그런데 sessionCreationPolicy 를 지정하면
                        // SessionManagementFilter 가 등록되어, JWT 필터가 인증을 채운 매 요청마다 이 전략이
                        // 실행된다. 폼을 렌더링하지 않는 JSON 응답(예: /api/tmdb/search) 은 새 토큰을 저장하지
                        // 않으므로 브라우저 쿠키만 사라지고, 이어지는 폼 POST 가 403 이 된다.
                        // 세션이 없어 토큰 고정(fixation) 회전이 의미 없으므로 no-op 전략으로 바꾼다.
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
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
                    .authorizationEndpoint(endpoint -> endpoint
                            .authorizationRequestRepository(authorizationRequestRepository))
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
