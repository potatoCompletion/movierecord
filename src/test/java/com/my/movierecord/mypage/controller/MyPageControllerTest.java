package com.my.movierecord.mypage.controller;

import com.my.movierecord.auth.oauth.CustomUserPrincipal;
import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.auth.service.CustomOAuth2UserService;
import com.my.movierecord.auth.service.UserService;
import com.my.movierecord.config.SecurityConfig;
import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.dto.RecordListItem;
import com.my.movierecord.record.dto.RecordPageDto;
import com.my.movierecord.record.dto.SortOption;
import com.my.movierecord.record.service.WatchRecordService;
import com.my.movierecord.record.stats.MyPageStats;
import com.my.movierecord.record.stats.MyPageStatsService;
import com.my.movierecord.support.WatchRecordFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MyPageController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class MyPageControllerTest {

    private static final MyPageStats EMPTY_STATS =
            new MyPageStats(0L, 0L, 0L, null, null, List.of(), 0L, List.of());

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WatchRecordService watchRecordService;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    UserService userService;

    @MockitoBean
    MyPageStatsService myPageStatsService;

    @MockitoBean
    CustomOAuth2UserService customOAuth2UserService;

    @BeforeEach
    void setUp() {
        given(userRepository.findByUsername(any(String.class)))
                .willReturn(Optional.of(WatchRecordFixture.createUser()));
        given(myPageStatsService.getStats(any(Long.class)))
                .willReturn(EMPTY_STATS);
    }

    @Test
    void GET_my_page_미인증_로그인_리다이렉트() throws Exception {
        mockMvc.perform(get("/my-page"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void GET_my_page_통계_탭_렌더링() throws Exception {
        mockMvc.perform(get("/my-page").with(user(mockPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("my-page/my-page"))
                .andExpect(model().attribute("activeTab", "stats"))
                .andExpect(model().attributeExists("stats", "currentUserId", "isAdmin"));
    }

    @Test
    void GET_my_page_records_목록_모달_렌더링() throws Exception {
        WatchRecord record = WatchRecordFixture.createWatchRecordWithId(1L);
        PageImpl<WatchRecord> page = new PageImpl<>(List.of(record), PageRequest.of(0, 20), 1);
        given(watchRecordService.listByUser(any(Long.class), any(Pageable.class)))
                .willReturn(RecordPageDto.of(page, page.map(RecordListItem::from).toList()));

        mockMvc.perform(get("/my-page/records").with(user(mockPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("my-page/my-page"))
                .andExpect(model().attribute("activeTab", "records"))
                .andExpect(model().attribute("currentSort", SortOption.LATEST))
                .andExpect(model().attributeExists("items", "page", "sortOptions", "currentUserId", "isAdmin"))
                .andExpect(result -> {
                    String html = result.getResponse().getContentAsString();
                    assertThat(html).contains(
                            "id=\"movieModal\"",
                            "src=\"/js/app.js\"",
                            "data-id=\"1\"",
                            "data-owner-id=\"1\"",
                            "data-rating=\"4.5\"");
                    assertThat(html).doesNotContain("location.href='/records/1'");
                });
    }

    private static CustomUserPrincipal mockPrincipal() {
        return new CustomUserPrincipal("user", "password", "테스트유저", "ROLE_USER");
    }
}
