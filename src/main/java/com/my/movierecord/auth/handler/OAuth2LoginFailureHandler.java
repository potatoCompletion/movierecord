package com.my.movierecord.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String url = "/auth/login?error";
        if (exception instanceof OAuth2AuthenticationException oae) {
            url = switch (oae.getError().getErrorCode()) {
                case "account_pending" -> "/auth/login?disabled";
                case "account_withdrawn" -> "/auth/login?withdrawn";
                default -> "/auth/login?error";
            };
        }
        response.sendRedirect(url);
    }
}
