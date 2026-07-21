package com.my.movierecord.auth.controller;

import com.my.movierecord.auth.dto.TokenPair;
import com.my.movierecord.auth.exception.InvalidRefreshTokenException;
import com.my.movierecord.auth.exception.UserAlreadyExistsException;
import com.my.movierecord.auth.security.CookieUtil;
import com.my.movierecord.auth.service.CustomOAuth2UserService;
import com.my.movierecord.auth.service.TokenService;
import com.my.movierecord.auth.service.UserService;
import com.my.movierecord.config.PasswordEncoderConfig;
import com.my.movierecord.config.SecurityConfig;
import com.my.movierecord.support.SecurityTestConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, SecurityTestConfig.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    CustomOAuth2UserService customOAuth2UserService;

    @Test
    void GET_auth_login_폼_렌더링() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void GET_auth_signup_폼_초기화() throws Exception {
        mockMvc.perform(get("/auth/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeExists("signupForm"));
    }

    @Test
    void POST_auth_signup_성공_리다이렉트() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "홍길동")
                        .param("username", "newuser")
                        .param("password", "pass1234")
                        .param("passwordConfirm", "pass1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?signupSuccess"));
    }

    @Test
    void POST_auth_signup_빈_이름_검증_실패() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "")
                        .param("username", "newuser")
                        .param("password", "pass1234")
                        .param("passwordConfirm", "pass1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("signupForm", "name"));
    }

    @Test
    void POST_auth_signup_빈_아이디_검증_실패() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "홍길동")
                        .param("username", "")
                        .param("password", "pass1234")
                        .param("passwordConfirm", "pass1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("signupForm", "username"));
    }

    @Test
    void POST_auth_signup_비밀번호_불일치_검증_실패() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "홍길동")
                        .param("username", "validuser")
                        .param("password", "pass1234")
                        .param("passwordConfirm", "different"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("signupForm", "passwordConfirm"));
    }

    @Test
    void POST_auth_signup_중복_아이디_검증_실패() throws Exception {
        willThrow(new UserAlreadyExistsException("existinguser"))
                .given(userService).signup(any());

        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "홍길동")
                        .param("username", "existinguser")
                        .param("password", "pass1234")
                        .param("passwordConfirm", "pass1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("signupForm", "username"));
    }

    // ===== POST /auth/token/refresh =====

    @Test
    void POST_token_refresh_성공시_새_쿠키_발급() throws Exception {
        given(tokenService.refresh(eq("old-refresh"))).willReturn(new TokenPair("new-access", "new-refresh"));

        mockMvc.perform(post("/auth/token/refresh")
                        .with(csrf())
                        .cookie(new Cookie(CookieUtil.REFRESH_TOKEN, "old-refresh")))
                .andExpect(status().isNoContent())
                .andExpect(result -> {
                    var setCookies = result.getResponse().getHeaders("Set-Cookie");
                    assertThat(setCookies).anyMatch(c -> c.startsWith(CookieUtil.ACCESS_TOKEN + "=new-access")
                            && c.contains("HttpOnly"));
                    assertThat(setCookies).anyMatch(c -> c.startsWith(CookieUtil.REFRESH_TOKEN + "=new-refresh")
                            && c.contains("HttpOnly") && c.contains("Path=/auth"));
                });
    }

    @Test
    void POST_token_refresh_쿠키없으면_401() throws Exception {
        mockMvc.perform(post("/auth/token/refresh").with(csrf()))
                .andExpect(status().isUnauthorized());
        then(tokenService).shouldHaveNoInteractions();
    }

    @Test
    void POST_token_refresh_재사용탐지시_401() throws Exception {
        willThrow(new InvalidRefreshTokenException("reuse")).given(tokenService).refresh(any());

        mockMvc.perform(post("/auth/token/refresh")
                        .with(csrf())
                        .cookie(new Cookie(CookieUtil.REFRESH_TOKEN, "revoked-refresh")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void POST_token_refresh_csrf없으면_403() throws Exception {
        mockMvc.perform(post("/auth/token/refresh")
                        .cookie(new Cookie(CookieUtil.REFRESH_TOKEN, "old-refresh")))
                .andExpect(status().isForbidden());
    }

    // ===== POST /auth/logout =====

    @Test
    void POST_logout_토큰폐기_및_쿠키만료() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .with(csrf())
                        .cookie(new Cookie(CookieUtil.REFRESH_TOKEN, "some-refresh")))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(result -> {
                    var setCookies = result.getResponse().getHeaders("Set-Cookie");
                    assertThat(setCookies).anyMatch(c -> c.startsWith(CookieUtil.ACCESS_TOKEN + "=")
                            && c.contains("Max-Age=0"));
                    assertThat(setCookies).anyMatch(c -> c.startsWith(CookieUtil.REFRESH_TOKEN + "=")
                            && c.contains("Max-Age=0"));
                });

        then(tokenService).should().revoke("some-refresh");
    }

    @Test
    void POST_logout_csrf없으면_403() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie(CookieUtil.REFRESH_TOKEN, "some-refresh")))
                .andExpect(status().isForbidden());
    }
}
