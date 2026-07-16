package com.my.movierecord.home.controller;

import com.my.movierecord.common.controller.HomeController;
import com.my.movierecord.kobis.service.KobisService;
import com.my.movierecord.record.repository.WatchRecordRepository;
import com.my.movierecord.spotlight.service.SpotlightService;
import com.my.movierecord.tmdb.service.TmdbHomeService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * HomeController 단위 테스트. standalone MockMvc 를 사용해 보안 필터/뷰 렌더링 없이
 * 컨트롤러의 뷰 이름과 모델 구성만 검증한다. 외부 연동 서비스는 모두 목으로 대체한다.
 */
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    KobisService kobisService;

    @Mock
    TmdbHomeService tmdbHomeService;

    @Mock
    SpotlightService spotlightService;

    @Mock
    WatchRecordRepository watchRecordRepository;

    @InjectMocks
    HomeController homeController;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();
    }

    @Test
    void GET_루트_home_뷰와_모델_구성() throws Exception {
        given(spotlightService.getSpotlights(any(LocalDate.class))).willReturn(List.of());
        given(kobisService.getDailyBoxOffice()).willReturn(List.of());
        given(tmdbHomeService.getNowPlaying()).willReturn(List.of());
        given(tmdbHomeService.getUpcoming()).willReturn(List.of());
        given(watchRecordRepository.findTopRated(any(LocalDateTime.class), any(Pageable.class)))
                .willReturn(List.of());
        given(watchRecordRepository.findTop4ByOrderByCreatedAtDesc()).willReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists(
                        "spotlights", "boxOffice", "boxOfficeBaseDayText",
                        "nowPlaying", "upcoming", "topRatings", "recentReviews"));
    }
}
