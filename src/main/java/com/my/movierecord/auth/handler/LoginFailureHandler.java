package com.my.movierecord.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String url;
        if (exception instanceof LockedException) {
            url = "/auth/login?withdrawn";
        } else if (exception instanceof DisabledException) {
            url = "/auth/login?disabled";
        } else {
            url = "/auth/login?error";
        }
        response.sendRedirect(url);
    }
}
