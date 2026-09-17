package com.my.movierecord.admin.controller;

import com.my.movierecord.auth.oauth.CustomUserPrincipal;
import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.auth.service.CustomOAuth2UserService;
import com.my.movierecord.auth.service.TokenService;
import com.my.movierecord.auth.service.UserService;
import com.my.movierecord.config.SecurityConfig;
import com.my.movierecord.support.SecurityTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 체험용 demo 계정(ROLE_USER)이 관리자 화면에 닿지 못하는지 SecurityConfig 규칙으로 확인한다.
 */
@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, SecurityTestConfig.class})
@ActiveProfiles("test")
class AdminControllerAccessTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    CustomOAuth2UserService customOAuth2UserService;

    @Test
    void ROLE_USER인_demo는_관리자_회원_목록에_403() throws Exception {
        mockMvc.perform(get("/admin/members").with(user(principal("demo", "데모", "ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void ROLE_ADMIN은_관리자_회원_목록에_200() throws Exception {
        given(userService.findAllUsers()).willReturn(List.of());
        given(userService.findWithdrawnUsers()).willReturn(List.of());

        mockMvc.perform(get("/admin/members").with(user(principal("admin", "관리자", "ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    /** 레이아웃 프래그먼트가 principal.displayNickname 을 읽으므로 실제 principal 타입을 쓴다. */
    private static CustomUserPrincipal principal(String username, String name, String role) {
        return new CustomUserPrincipal(username, "password", name, role);
    }
}
