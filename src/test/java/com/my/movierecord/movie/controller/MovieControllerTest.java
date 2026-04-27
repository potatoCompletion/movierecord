package com.my.movierecord.movie.controller;

import com.my.movierecord.common.service.FileStorageService;
import com.my.movierecord.config.SecurityConfig;
import com.my.movierecord.movie.domain.Movie;
import com.my.movierecord.movie.dto.SortOption;
import com.my.movierecord.movie.service.MovieSaveCommand;
import com.my.movierecord.movie.service.MovieService;
import com.my.movierecord.support.MovieFixture;
import java.util.List;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MovieController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@TestPropertySource(properties = "app.upload.dir=${java.io.tmpdir}/movierecord-test")
class MovieControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MovieService movieService;

    @MockitoBean
    FileStorageService fileStorageService;

    // ===== GET /movies =====

    @Test
    void GET_movies_미인증_로그인_리다이렉트() throws Exception {
        mockMvc.perform(get("/movies"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void GET_movies_200_목록_반환() throws Exception {
        Movie movie = MovieFixture.createMovieWithId(1L);
        given(movieService.list(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(movie), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/movies").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/list"))
                .andExpect(model().attributeExists("items", "page", "currentSort", "sortOptions"))
                .andDo(document("movies/list"));
    }

    @Test
    void GET_movies_sort_파라미터_적용() throws Exception {
        given(movieService.list(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/movies").param("sort", "rating").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentSort", SortOption.RATING));
    }

    @Test
    void GET_movies_잘못된_sort_LATEST_기본값() throws Exception {
        given(movieService.list(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/movies").param("sort", "unknown").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentSort", SortOption.LATEST));
    }

    // ===== GET /movies/new =====

    @Test
    void GET_movies_new_폼_초기화() throws Exception {
        mockMvc.perform(get("/movies/new").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/form"))
                .andExpect(model().attribute("mode", "create"))
                .andExpect(model().attributeExists("movieForm", "immersionOptions", "storyOptions", "emotionOptions", "tasteOptions"))
                .andDo(document("movies/new-form"));
    }

    // ===== GET /movies/{id}/edit =====

    @Test
    void GET_movies_id_edit_기존값_채워짐() throws Exception {
        Movie movie = MovieFixture.createMovieWithThumbnail(1L, "thumb.jpg");
        given(movieService.get(1L)).willReturn(movie);

        mockMvc.perform(get("/movies/1/edit").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/form"))
                .andExpect(model().attribute("mode", "edit"))
                .andExpect(model().attribute("movieId", 1L))
                .andExpect(model().attribute("existingThumbnailUrl", "/uploads/thumb.jpg"))
                .andDo(document("movies/edit-form"));
    }

    @Test
    void GET_movies_id_edit_썸네일_없으면_url_null() throws Exception {
        Movie movie = MovieFixture.createMovieWithId(1L);
        given(movieService.get(1L)).willReturn(movie);

        mockMvc.perform(get("/movies/1/edit").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(result ->
                        assertThat(result.getModelAndView().getModel().get("existingThumbnailUrl")).isNull());
    }

    // ===== POST /movies (등록) =====

    @Test
    void POST_movies_csrf_없으면_403() throws Exception {
        mockMvc.perform(post("/movies")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("title", "영화"))
                .andExpect(status().isForbidden());
    }

    @Test
    void POST_movies_유효한_폼_등록_리다이렉트() throws Exception {
        mockMvc.perform(post("/movies")
                .with(csrf()).with(user("user"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(validFormParams()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"))
                .andExpect(flash().attribute("message", "영화가 등록되었습니다."))
                .andDo(document("movies/create"));
    }

    @Test
    void POST_movies_제목_공백_검증_실패() throws Exception {
        org.springframework.util.LinkedMultiValueMap<String, String> params = validFormParams();
        params.set("title", "");

        mockMvc.perform(post("/movies")
                .with(csrf()).with(user("user"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/form"))
                .andExpect(model().attributeHasFieldErrors("movieForm", "title"));
    }

    @Test
    void POST_movies_별점_범위_초과_검증_실패() throws Exception {
        org.springframework.util.LinkedMultiValueMap<String, String> params = validFormParams();
        params.set("rating", "6.0");

        mockMvc.perform(post("/movies")
                .with(csrf()).with(user("user"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/form"))
                .andExpect(model().attributeHasFieldErrors("movieForm", "rating"));
    }

    @Test
    void POST_movies_썸네일_있으면_store_호출() throws Exception {
        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnail", "test.jpg", "image/jpeg", "image-content".getBytes());
        given(fileStorageService.store(any())).willReturn("uuid.jpg");

        mockMvc.perform(multipart("/movies")
                .file(thumbnail)
                .with(csrf()).with(user("user"))
                .params(validFormParams()))
                .andExpect(status().is3xxRedirection());

        then(fileStorageService).should().store(any());
    }

    @Test
    void POST_movies_썸네일_없으면_store_null_로_create_호출() throws Exception {
        mockMvc.perform(post("/movies")
                .with(csrf()).with(user("user"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(validFormParams()))
                .andExpect(status().is3xxRedirection());

        then(movieService).should().create(any(MovieSaveCommand.class));
    }

    // ===== POST /movies/{id} (수정) =====

    @Test
    void POST_movies_id_수정_성공_리다이렉트() throws Exception {
        mockMvc.perform(post("/movies/1")
                .with(csrf()).with(user("user"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(validFormParams()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"))
                .andExpect(flash().attribute("message", "영화가 수정되었습니다."))
                .andDo(document("movies/update"));
    }

    @Test
    void POST_movies_id_수정_검증_실패_existingThumbnailUrl_복원() throws Exception {
        Movie movie = MovieFixture.createMovieWithThumbnail(1L, "thumb.jpg");
        given(movieService.get(1L)).willReturn(movie);

        org.springframework.util.LinkedMultiValueMap<String, String> params = validFormParams();
        params.set("title", "");

        mockMvc.perform(post("/movies/1")
                .with(csrf()).with(user("user"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/form"))
                .andExpect(model().attribute("existingThumbnailUrl", "/uploads/thumb.jpg"));

        then(movieService).should().get(1L);
    }

    @Test
    void POST_movies_id_수정_새_썸네일_replaceThumbnail_true() throws Exception {
        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnail", "new.jpg", "image/jpeg", "image-content".getBytes());
        given(fileStorageService.store(any())).willReturn("new-uuid.jpg");

        mockMvc.perform(multipart("/movies/1")
                .file(thumbnail)
                .with(csrf()).with(user("user"))
                .params(validFormParams()))
                .andExpect(status().is3xxRedirection());

        then(movieService).should().update(eq(1L), any(MovieSaveCommand.class), eq(true));
    }

    @Test
    void POST_movies_id_수정_썸네일_없으면_replaceThumbnail_false() throws Exception {
        mockMvc.perform(post("/movies/1")
                .with(csrf()).with(user("user"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(validFormParams()))
                .andExpect(status().is3xxRedirection());

        then(movieService).should().update(eq(1L), any(MovieSaveCommand.class), eq(false));
    }

    // ===== POST /movies/{id}/delete =====

    @Test
    void POST_movies_id_delete_성공_리다이렉트() throws Exception {
        mockMvc.perform(post("/movies/1/delete")
                .with(csrf()).with(user("user")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"))
                .andExpect(flash().attribute("message", "영화가 삭제되었습니다."))
                .andDo(document("movies/delete"));
    }

    @Test
    void POST_movies_id_delete_csrf_없으면_403() throws Exception {
        mockMvc.perform(post("/movies/1/delete")
                .with(user("user")))
                .andExpect(status().isForbidden());
    }

    // ===== 헬퍼 =====

    private org.springframework.util.LinkedMultiValueMap<String, String> validFormParams() {
        org.springframework.util.LinkedMultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
        params.add("title", "테스트 영화");
        params.add("watchedDate", "2024-06-01");
        params.add("immersion", "GOOD");
        params.add("story", "CONVINCING");
        params.add("emotion", "FUNNY");
        params.add("taste", "MATCH");
        params.add("rating", "4.5");
        return params;
    }
}
