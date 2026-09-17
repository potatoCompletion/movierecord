package com.my.movierecord.tmdb.dto;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpcomingItemTest {

    private static final LocalDate FIRST = LocalDate.of(2025, 9, 1);

    private static UpcomingCandidate candidate(LocalDate firstReleaseDate) {
        return new UpcomingCandidate(1L, "제목", "Title", "/p.jpg", "https://img/p.jpg", firstReleaseDate);
    }

    @Test
    void KR_개봉일이_있으면_그_날짜를_쓴다() {
        UpcomingItem item = UpcomingItem.from(candidate(FIRST), LocalDate.of(2026, 10, 1));

        assertThat(item.releaseDate()).isEqualTo("2026-10-01");
        assertThat(item.id()).isEqualTo(1L);
        assertThat(item.title()).isEqualTo("제목");
        assertThat(item.posterUrl()).isEqualTo("https://img/p.jpg");
    }

    @Test
    void KR_개봉일이_null_이면_discover_최초_개봉일로_폴백한다() {
        UpcomingItem item = UpcomingItem.from(candidate(FIRST), null);

        assertThat(item.releaseDate()).isEqualTo("2025-09-01");
        assertThat(item.reRelease()).isFalse();
    }

    @Test
    void 최초_개봉일보다_1년_이상_늦으면_재개봉이다() {
        UpcomingItem item = UpcomingItem.from(candidate(FIRST), FIRST.plusDays(365));

        assertThat(item.reRelease()).isTrue();
    }

    @Test
    void 최초_개봉일과_364일_차이면_재개봉이_아니다() {
        UpcomingItem item = UpcomingItem.from(candidate(FIRST), FIRST.plusDays(364));

        assertThat(item.reRelease()).isFalse();
    }
}
