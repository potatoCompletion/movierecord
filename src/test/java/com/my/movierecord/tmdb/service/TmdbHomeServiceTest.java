package com.my.movierecord.tmdb.service;

import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.UpcomingCandidate;
import com.my.movierecord.tmdb.dto.UpcomingItem;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TmdbHomeServiceTest {

    @Mock
    TmdbClient tmdbClient;

    @InjectMocks
    TmdbHomeService tmdbHomeService;

    private static UpcomingCandidate candidate(long id, LocalDate firstReleaseDate) {
        return new UpcomingCandidate(id, "제목" + id, "Title" + id, "/p.jpg", "https://img/p.jpg", firstReleaseDate);
    }

    @Test
    void 후보마다_KR_개봉일을_조회하고_실패한_건만_discover_날짜로_폴백한다() {
        LocalDate far = LocalDate.now().plusDays(20);
        LocalDate near = LocalDate.now().plusDays(5);
        given(tmdbClient.getUpcoming()).willReturn(List.of(
                candidate(1L, LocalDate.of(2019, 5, 30)),   // 재개봉작: KR 조회 성공
                candidate(2L, far)));                       // KR 조회 실패(null) → discover 날짜
        given(tmdbClient.getKoreanReleaseDate(eq(1L), any(LocalDate.class))).willReturn(near);
        given(tmdbClient.getKoreanReleaseDate(eq(2L), any(LocalDate.class))).willReturn(null);

        List<UpcomingItem> items = tmdbHomeService.getUpcoming();

        assertThat(items).extracting(UpcomingItem::id).containsExactly(1L, 2L);
        assertThat(items.get(0).releaseDate()).isEqualTo(near.toString());
        assertThat(items.get(0).reRelease()).isTrue();
        assertThat(items.get(1).releaseDate()).isEqualTo(far.toString());
        assertThat(items.get(1).reRelease()).isFalse();
        verify(tmdbClient).getKoreanReleaseDate(eq(1L), eq(LocalDate.now()));
    }

    @Test
    void 결과는_KR_개봉일_오름차순이다() {
        LocalDate d1 = LocalDate.now().plusDays(3);
        LocalDate d2 = LocalDate.now().plusDays(10);
        given(tmdbClient.getUpcoming()).willReturn(List.of(candidate(1L, d2), candidate(2L, d1)));
        given(tmdbClient.getKoreanReleaseDate(eq(1L), any(LocalDate.class))).willReturn(d2);
        given(tmdbClient.getKoreanReleaseDate(eq(2L), any(LocalDate.class))).willReturn(d1);

        List<UpcomingItem> items = tmdbHomeService.getUpcoming();

        assertThat(items).extracting(UpcomingItem::id).containsExactly(2L, 1L);
    }

    @Test
    void 후보가_없으면_빈_목록이다() {
        given(tmdbClient.getUpcoming()).willReturn(List.of());

        assertThat(tmdbHomeService.getUpcoming()).isEmpty();
    }
}
