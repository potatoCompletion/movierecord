package com.my.movierecord.auth.security;

import com.my.movierecord.auth.oauth.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 요청마다 {@code ACCESS_TOKEN} 쿠키의 JWT를 검증해, 유효하면 {@link CustomUserPrincipal} 기반
 * 인증을 SecurityContext 에 설정한다. 세션을 사용하지 않으므로(STATELESS) 매 요청 재구성한다.
 * 하위 컨트롤러의 {@code @AuthenticationPrincipal} 패턴은 그대로 동작한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, CookieUtil cookieUtil) {
        this.jwtProvider = jwtProvider;
        this.cookieUtil = cookieUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            cookieUtil.readAccessToken(request)
                    .flatMap(jwtProvider::parseAccessToken)
                    .ifPresent(claims -> authenticate(claims, request));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(JwtProvider.AccessTokenClaims claims, HttpServletRequest request) {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                claims.username(), null, claims.nickname(), claims.role());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
