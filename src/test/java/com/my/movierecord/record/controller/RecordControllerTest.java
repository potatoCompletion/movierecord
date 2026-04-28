package com.my.movierecord.record.controller;

import com.my.movierecord.auth.oauth.CustomUserPrincipal;
import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.config.SecurityConfig;
import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.dto.SortOption;
import com.my.movierecord.record.service.WatchRecordSaveCommand;
import com.my.movierecord.record.service.WatchRecordService;
import com.my.movierecord.support.WatchRecordFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RecordController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class RecordControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WatchRecordService watchRecordService;

    @MockitoBean
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        given(userRepository.findByUsername(any(String.class)))
                .willReturn(Optional.of(WatchRecordFixture.createUser()));
    }

    // ===== GET /contents =====

    @Test
    void GET_contents_미인증_로그인_리다이렉트() throws Exception {
        mockMvc.perform(get("/contents"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void GET_contents_200_목록_반환() throws Exception {
        WatchRecord record = WatchRecordFixture.createWatchRecordWithId(1L);
        given(watchRecordService.list(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(record), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/contents").with(user(mockPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("contents/list"))
                .andExpect(model().attributeExists("items", "page", "currentSort", "sortOptions"))
                .andExpect(result -> {
                    String html = result.getResponse().getContentAsString();
                    assertThat(html).contains(
                            "id=\"movieModal\"",
                            "src=\"/js/app.js\"",
                            "data-id=\"1\"",
                            "data-owner-id=\"1\"",
                            "data-rating=\"4.5\"");
                    assertThat(html).doesNotContain("location.href='/contents/1'");
                })
                .andDo(document("contents/list"));
    }

    @Test
    void GET_contents_sort_파라미터_적용() throws Exception {
        given(watchRecordService.list(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/contents").param("sort", "rating").with(user(mockPrincipal())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentSort", SortOption.RATING));
    }

    @Test
    void GET_contents_잘못된_sort_LATEST_기본값() throws Exception {
        given(watchRecordService.list(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/contents").param("sort", "unknown").with(user(mockPrincipal())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentSort", SortOption.LATEST));
    }

    // ===== GET /contents/new =====

    @Test
    void GET_contents_new_폼_초기화() throws Exception {
        mockMvc.perform(get("/contents/new").with(user(mockPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("contents/form"))
                .andExpect(model().attribute("mode", "create"))
                .andExpect(model().attributeExists("movieForm", "immersionOptions", "storyOptions", "emotionOptions", "tasteOptions"))
                .andDo(document("contents/new-form"));
    }

    // ===== GET /contents/{id}/edit =====

    @Test
    void GET_contents_id_edit_기존값_채워짐() throws Exception {
        WatchRecord record = WatchRecordFixture.createWatchRecordWithContent(1L, "thumb.jpg");
        given(watchRecordService.get(1L)).willReturn(record);

        mockMvc.perform(get("/contents/1/edit").with(user(mockPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("contents/form"))
                .andExpect(model().attribute("mode", "edit"))
                .andExpect(model().attribute("movieId", 1L))
                .andExpect(model().attribute("existingThumbnailUrl", "/uploads/thumb.jpg"))
                .andDo(document("contents/edit-form"));
    }

    @Test
    void GET_contents_id_edit_썸네일_없으면_url_null() throws Exception {
        WatchRecord record = WatchRecordFixture.createWatchRecordWithId(1L);
        given(watchRecordService.get(1L)).willReturn(record);

        mockMvc.perform(get("/contents/1/edit").with(user(mockPrincipal())))
                .andExpect(status().isOk())
                .andExpect(result ->
                        assertThat(result.getModelAndView().getModel().get("existingThumbnailUrl")).isNull());
    }

    // ===== POST /contents (등록) =====

    @Test
    void POST_contents_csrf_없으면_403() throws Exception {
        mockMvc.perform(post("/contents")
                .with(user(mockPrincipal()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("title", "영화"))
                .andExpect(status().isForbidden());
    }

    @Test
    void POST_contents_유효한_폼_등록_리다이렉트() throws Exception {
        mockMvc.perform(post("/contents")
                .with(csrf()).with(user(mockPrincipal()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(validFormParams()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contents"))
                .andExpect(flash().attribute("success", "감상평이 등록되었습니다."))
                .andDo(document("contents/create"));
    }

    @Test
    void POST_contents_제목_공백_검증_실패() throws Exception {
        org.springframework.util.LinkedMultiValueMap<String, String> params = validFormParams();
        params.set("title", "");

        mockMvc.perform(post("/contents")
                .with(csrf()).with(user(mockPrincipal()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("contents/form"))
                .andExpect(model().attributeHasFieldErrors("movieForm", "title"));
    }

    @Test
    void POST_contents_별점_범위_초과_검증_실패() throws Exception {
        org.springframework.util.LinkedMultiValueMap<String, String> params = validFormParams();
        params.set("rating", "6.0");

        mockMvc.perform(post("/contents")
                .with(csrf()).with(user(mockPrincipal()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("contents/form"))
                .andExpect(model().attributeHasFieldErrors("movieForm", "rating"));
    }

    @Test
    void POST_contents_create_service_호출() throws Exception {
        mockMvc.perform(post("/contents")
                .with(csrf()).with(user(mockPrincipal()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(validFormParams()))
                .andExpect(status().is3xxRedirection());

        then(watchRecordService).should().create(any(WatchRecordSaveCommand.class));
    }

    // ===== POST /contents/{id} (수정) =====

    @Test
    void POST_contents_id_수정_성공_리다이렉트() throws Exception {
        given(watchRecordService.get(1L)).willReturn(WatchRecordFixture.createWatchRecordWithId(1L));

        mockMvc.perform(post("/contents/1")
                .with(csrf()).with(user(mockPrincipal()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(validFormParams()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contents"))
                .andExpect(flash().attribute("success", "감상평이 수정되었습니다."))
                .andDo(document("contents/update"));
    }

    @Test
    void POST_contents_id_수정_service_호출() throws Exception {
        given(watchRecordService.get(1L)).willReturn(WatchRecordFixture.createWatchRecordWithId(1L));

        mockMvc.perform(post("/contents/1")
                .with(csrf()).with(user(mockPrincipal()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(validFormParams()))
                .andExpect(status().is3xxRedirection());

        then(watchRecordService).should().update(eq(1L), any(WatchRecordSaveCommand.class));
    }

    @Test
    void POST_contents_id_수정_검증_실패_existingThumbnailUrl_복원() throws Exception {
        WatchRecord record = WatchRecordFixture.createWatchRecordWithContent(1L, "thumb.jpg");
        given(watchRecordService.get(1L)).willReturn(record);

        org.springframework.util.LinkedMultiValueMap<String, String> params = validFormParams();
        params.set("title", "");

        mockMvc.perform(post("/contents/1")
                .with(csrf()).with(user(mockPrincipal()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("contents/form"))
                .andExpect(model().attribute("existingThumbnailUrl", "/uploads/thumb.jpg"));

        then(watchRecordService).should().get(1L);
    }

    // ===== POST /contents/{id}/delete =====

    @Test
    void POST_contents_id_delete_성공_리다이렉트() throws Exception {
        given(watchRecordService.get(1L)).willReturn(WatchRecordFixture.createWatchRecordWithId(1L));

        mockMvc.perform(post("/contents/1/delete")
                .with(csrf()).with(user(mockPrincipal())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contents"))
                .andExpect(flash().attribute("success", "감상평이 삭제되었습니다."))
                .andDo(document("contents/delete"));
    }

    @Test
    void POST_contents_id_delete_csrf_없으면_403() throws Exception {
        mockMvc.perform(post("/contents/1/delete")
                .with(user(mockPrincipal())))
                .andExpect(status().isForbidden());
    }

    // ===== 헬퍼 =====

    private static CustomUserPrincipal mockPrincipal() {
        return new CustomUserPrincipal("user", "password", "테스트유저", "ROLE_USER");
    }

    private org.springframework.util.LinkedMultiValueMap<String, String> validFormParams() {
        org.springframework.util.LinkedMultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
        params.add("title", "테스트 영화");
        params.add("watchedDate", "2024-06-01");
        params.add("immersion", "GOOD");
        params.add("story", "CONVINCING");
        params.add("emotions", "FUNNY");
        params.add("taste", "MATCH");
        params.add("rating", "4.5");
        return params;
    }
}
