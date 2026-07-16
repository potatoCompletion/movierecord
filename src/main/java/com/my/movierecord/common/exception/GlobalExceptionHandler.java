package com.my.movierecord.common.exception;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String VIEW_404 = "error/404";
    private static final String VIEW_500 = "error/500";
    private static final String VIEW_503 = "error/503";

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handle404(NoResourceFoundException ex, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        log.warn("404 Not Found: {}", request.getRequestURI());
        return render(request, response, HttpStatus.NOT_FOUND, VIEW_404,
                "요청한 리소스를 찾을 수 없습니다.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView handle405(HttpRequestMethodNotSupportedException ex, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        log.warn("405 Method Not Allowed: {} {}", request.getMethod(), request.getRequestURI());
        return render(request, response, HttpStatus.METHOD_NOT_ALLOWED, VIEW_404,
                "허용되지 않는 HTTP 메서드입니다.");
    }

    /** 도메인 엔티티 미조회 및 외부 API의 4xx(잘못된 요청/미존재) → 404. */
    @ExceptionHandler({EntityNotFoundException.class, ExternalApiClientException.class})
    public ModelAndView handleNotFound(Exception ex, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        log.warn("404 Not Found at {}: {}", request.getRequestURI(), ex.toString());
        return render(request, response, HttpStatus.NOT_FOUND, VIEW_404,
                "요청한 정보를 찾을 수 없습니다.");
    }

    /** 잘못된 인자/검증 실패 → 400. */
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            BindException.class})
    public ModelAndView handleBadRequest(Exception ex, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        log.warn("400 Bad Request at {}: {}", request.getRequestURI(), ex.toString());
        return render(request, response, HttpStatus.BAD_REQUEST, VIEW_500,
                "잘못된 요청입니다.");
    }

    /** 접근 거부 → 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleForbidden(AccessDeniedException ex, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        log.warn("403 Forbidden at {}: {}", request.getRequestURI(), ex.toString());
        return render(request, response, HttpStatus.FORBIDDEN, VIEW_500,
                "접근 권한이 없습니다.");
    }

    /** 외부 API 일시 장애(재시도 소진)·전송 오류(타임아웃)·서킷 오픈·레이트리밋·벌크헤드 포화 → 503. */
    @ExceptionHandler({ExternalApiTransientException.class, ResourceAccessException.class,
            CallNotPermittedException.class, RequestNotPermitted.class, BulkheadFullException.class})
    public ModelAndView handleServiceUnavailable(Exception ex, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        log.error("503 Service Unavailable at {}: {}", request.getRequestURI(), ex.toString());
        return render(request, response, HttpStatus.SERVICE_UNAVAILABLE, VIEW_503,
                "일시적으로 외부 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handle500(Exception ex, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        log.error("500 Internal Server Error at {}", request.getRequestURI(), ex);
        return render(request, response, HttpStatus.INTERNAL_SERVER_ERROR, VIEW_500,
                "서버 오류가 발생했습니다.");
    }

    private ModelAndView render(HttpServletRequest request, HttpServletResponse response,
            HttpStatus status, String view, String message) throws IOException {
        if (isRestRequest(request)) {
            writeJson(response, status, message, request.getRequestURI());
            return null;
        }
        ModelAndView mav = new ModelAndView(view);
        mav.setStatus(status);
        return mav;
    }

    private boolean isRestRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/")
                || (accept != null && accept.contains("application/json") && !accept.contains("text/html"));
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, String message, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiErrorResponse body = ApiErrorResponse.of(status, message, path);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
