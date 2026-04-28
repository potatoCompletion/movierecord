package com.my.movierecord.config;

import com.my.movierecord.auth.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/login",
                                "/auth/signup",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/contents", true)
                        .failureHandler((request, response, exception) -> {
                            String url = (exception instanceof DisabledException)
                                    ? "/auth/login?disabled"
                                    : "/auth/login?error";
                            response.sendRedirect(url);
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/auth/login?logout")
                        .permitAll()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/auth/login")
                    .defaultSuccessUrl("/contents", true)
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                    )
                    .failureHandler((request, response, exception) -> {
                        String url = (exception instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException oae
                                && "account_pending".equals(oae.getError().getErrorCode()))
                                ? "/auth/login?disabled"
                                : "/auth/login?error";
                        response.sendRedirect(url);
                    })
            );
        }

        return http.build();
    }
}
