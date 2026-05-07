package com.my.movierecord.home.controller;

import com.my.movierecord.auth.service.CustomOAuth2UserService;
import com.my.movierecord.common.controller.HomeController;
import com.my.movierecord.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class HomeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CustomOAuth2UserService customOAuth2UserService;

    @Test
    void GET_루트_미인증_로그인_리다이렉트() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void GET_루트_인증_records_리다이렉트() throws Exception {
        mockMvc.perform(get("/").with(user("user")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/records"));
    }
}
