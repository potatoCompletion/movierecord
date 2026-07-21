package com.my.movierecord.auth.security;

import com.my.movierecord.common.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 미인증 접근 처리. {@code GlobalExceptionHandler.isRestRequest()}와 동일한 판단으로
 * 브라우저 요청은 로그인 페이지로 리다이렉트하고, API 요청은 401 JSON 을 반환한다.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (isRestRequest(request)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            ApiErrorResponse body = ApiErrorResponse.of(
                    HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", request.getRequestURI());
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }
        response.sendRedirect("/auth/login");
    }

    private boolean isRestRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/")
                || (accept != null && accept.contains("application/json") && !accept.contains("text/html"));
    }
}
