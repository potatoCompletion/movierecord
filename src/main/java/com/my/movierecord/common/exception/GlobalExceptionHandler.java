package com.my.movierecord.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handle404(NoResourceFoundException ex, HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.warn("404 Not Found: {}", request.getRequestURI());
        if (isRestRequest(request)) {
            writeJson(response, HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.", request.getRequestURI());
            return null;
        }
        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView handle405(HttpRequestMethodNotSupportedException ex, HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.warn("405 Method Not Allowed: {} {}", request.getMethod(), request.getRequestURI());
        if (isRestRequest(request)) {
            writeJson(response, HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않는 HTTP 메서드입니다.", request.getRequestURI());
            return null;
        }
        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.METHOD_NOT_ALLOWED);
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handle500(Exception ex, HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.error("500 Internal Server Error at {}", request.getRequestURI(), ex);
        if (isRestRequest(request)) {
            writeJson(response, HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", request.getRequestURI());
            return null;
        }
        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }

    private boolean isRestRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/")
                || (accept != null && accept.contains("application/json") && !accept.contains("text/html"));
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                status.value(),
                status.getReasonPhrase(),
                message.replace("\"", "\\\""),
                path.replace("\"", "\\\"")
        );
        response.getWriter().write(json);
    }
}
