package com.my.movierecord.home.controller;

import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.auth.service.CustomOAuth2UserService;
import com.my.movierecord.auth.service.UserService;
import com.my.movierecord.common.controller.HomeController;
import com.my.movierecord.config.SecurityConfig;
import com.my.movierecord.kobis.dto.BoxOfficeItemDto;
import com.my.movierecord.kobis.service.KobisService;
import com.my.movierecord.record.repository.WatchRecordRepository;
import com.my.movierecord.spotlight.service.SpotlightService;
import com.my.movierecord.support.SecurityTestConfig;
import com.my.movierecord.tmdb.dto.UpcomingItem;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;
import com.my.movierecord.tmdb.service.TmdbHomeService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 홈 템플릿 렌더 결과를 검증한다. 모델 구성은 {@link HomeControllerTest}가 다루고,
 * 여기서는 D-Day/재개봉 뱃지와 박스오피스 문구가 실제 HTML 에 어떻게 찍히는지만 본다.
 */
@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, SecurityTestConfig.class, TmdbImageUrlProvider.class})
@ActiveProfiles("test")
class HomeControllerRenderTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KobisService kobisService;

    @MockitoBean
    TmdbHomeService tmdbHomeService;

    @MockitoBean
    SpotlightService spotlightService;

    @MockitoBean
    WatchRecordRepository watchRecordRepository;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    UserService userService;

    @MockitoBean
    CustomOAuth2UserService customOAuth2UserService;

    @Test
    void 홈은_재개봉_뱃지와_전일_기준_문구를_렌더하고_음수_DDay가_없다() throws Exception {
        LocalDate today = LocalDate.now();
        given(spotlightService.getSpotlights(any(LocalDate.class))).willReturn(List.of());
        given(kobisService.getDailyBoxOffice()).willReturn(List.of(
                new BoxOfficeItemDto(1, "영화", null, 1000L, null, null)));
        given(tmdbHomeService.getNowPlaying()).willReturn(List.of());
        given(tmdbHomeService.getUpcoming()).willReturn(List.of(
                new UpcomingItem(1L, "지난 작품", "Past", null, null, today.minusDays(2).toString(), false),
                new UpcomingItem(2L, "오늘 개봉", "Today", null, null, today.toString(), false),
                new UpcomingItem(3L, "재개봉작", "Again", null, null, today.plusDays(3).toString(), true)));
        given(watchRecordRepository.findTopRated(any(LocalDateTime.class), any(Pageable.class)))
                .willReturn(List.of());
        given(watchRecordRepository.findTop4ByOrderByCreatedAtDesc()).willReturn(List.of());

        String html = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("전일 기준", "D-DAY", "재개봉 · D-3", "up-dday--rerelease");
        assertThat(html).doesNotContain("실시간", "D--", "지난 작품");
    }
}
