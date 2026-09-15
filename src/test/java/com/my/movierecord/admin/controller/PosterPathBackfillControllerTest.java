package com.my.movierecord.admin.controller;

import com.my.movierecord.admin.dto.PosterPathBackfillResult;
import com.my.movierecord.admin.service.PosterPathBackfillService;
import com.my.movierecord.auth.service.CustomOAuth2UserService;
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
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PosterPathBackfillController.class)
@Import({SecurityConfig.class, SecurityTestConfig.class})
@ActiveProfiles("test")
class PosterPathBackfillControllerTest {

    private static final String URL = "/admin/backfill/poster-path";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PosterPathBackfillService posterPathBackfillService;

    @MockitoBean
    CustomOAuth2UserService customOAuth2UserService;

    @Test
    void POST_관리자는_집계_JSON을_받는다() throws Exception {
        given(posterPathBackfillService.backfill())
                .willReturn(new PosterPathBackfillResult(3, 2, 0, 1, List.of("movie:45")));

        mockMvc.perform(post(URL).with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(3))
                .andExpect(jsonPath("$.succeeded").value(2))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.failedIds[0]").value("movie:45"));
    }

    @Test
    void POST_일반_사용자는_403이고_백필이_실행되지_않는다() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).with(user("user").roles("USER")))
                .andExpect(status().isForbidden());

        then(posterPathBackfillService).shouldHaveNoInteractions();
    }

    @Test
    void POST_미인증은_거부되고_백필이_실행되지_않는다() throws Exception {
        mockMvc.perform(post(URL).with(csrf()))
                .andExpect(status().is3xxRedirection());

        then(posterPathBackfillService).shouldHaveNoInteractions();
    }
}
