package com.my.movierecord.config;

import com.my.movierecord.auth.controller.AuthController;
import com.my.movierecord.auth.security.CookieUtil;
import com.my.movierecord.auth.security.JwtProvider;
import com.my.movierecord.auth.service.CustomOAuth2UserService;
import com.my.movierecord.auth.service.TokenService;
import com.my.movierecord.auth.service.UserService;
import com.my.movierecord.support.SecurityTestConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * JWT 쿠키로 인증된 요청이 CSRF 토큰 쿠키({@code XSRF-TOKEN})를 건드리지 않는지 검증한다.
 *
 * <p>회귀 배경: {@code sessionCreationPolicy(STATELESS)}만 지정하면 {@code SessionManagementFilter}가
 * 등록되어 매 요청 {@code CsrfAuthenticationStrategy}를 실행했고, 이 전략이 기존 XSRF-TOKEN 쿠키를
 * 삭제(Max-Age=0)한 뒤 새 토큰은 지연 생성으로 남겨뒀다. 폼을 렌더링하지 않는 JSON 응답에서는 새 토큰이
 * 저장되지 않아 브라우저의 쿠키가 사라지고, 이어지는 폼 POST가 403이 됐다.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, SecurityTestConfig.class})
@ActiveProfiles("test")
class SecurityConfigCsrfCookieTest {

    private static final String XSRF_COOKIE = "XSRF-TOKEN";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtProvider jwtProvider;

    @MockitoBean
    UserService userService;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    CustomOAuth2UserService customOAuth2UserService;

    @Test
    void JWT_인증_요청은_기존_XSRF_TOKEN_쿠키를_삭제하지_않는다() throws Exception {
        String accessToken = jwtProvider.createAccessToken("admin", 1L, "ROLE_ADMIN", "관리자");

        // 폼을 렌더링하지 않는(=CSRF 토큰을 읽지 않는) 인증 필요 경로. 매핑이 없어도 필터 체인은 모두 통과한다.
        MvcResult result = mockMvc.perform(get("/api/anything")
                        .cookie(new Cookie(CookieUtil.ACCESS_TOKEN, accessToken))
                        .cookie(new Cookie(XSRF_COOKIE, "existing-token")))
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);

        assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
        assertThat(setCookies)
                .as("XSRF-TOKEN 쿠키를 삭제하거나 교체하는 Set-Cookie 가 없어야 한다")
                .noneMatch(value -> value.startsWith(XSRF_COOKIE + "="));
    }
}
