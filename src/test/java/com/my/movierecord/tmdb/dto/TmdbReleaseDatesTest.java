package com.my.movierecord.tmdb.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TmdbReleaseDatesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    @Test
    void KR_type3_항목의_날짜를_고른다() {
        Map<String, Object> body = body(
                country("US", entry(3, "2026-09-20T00:00:00.000Z")),
                country("KR", entry(3, "2026-10-01T00:00:00.000Z")));

        Optional<LocalDate> result = TmdbReleaseDates.koreanTheatricalDate(body, TODAY);

        assertThat(result).contains(LocalDate.of(2026, 10, 1));
    }

    @Test
    void type3가_여러_개면_오늘_이후_가장_이른_날짜를_고른다() {
        // 재개봉작: 원 개봉(과거)과 재개봉(미래) type 3 이 함께 온다.
        Map<String, Object> body = body(country("KR",
                entry(3, "2019-05-30T00:00:00.000Z"),
                entry(3, "2026-10-15T00:00:00.000Z"),
                entry(3, "2026-10-01T00:00:00.000Z")));

        Optional<LocalDate> result = TmdbReleaseDates.koreanTheatricalDate(body, TODAY);

        assertThat(result).contains(LocalDate.of(2026, 10, 1));
    }

    @Test
    void 오늘_당일은_이후로_취급한다() {
        Map<String, Object> body = body(country("KR", entry(3, "2026-09-17T00:00:00.000Z")));

        assertThat(TmdbReleaseDates.koreanTheatricalDate(body, TODAY)).contains(TODAY);
    }

    @Test
    void type3가_없으면_type2로_폴백한다() {
        Map<String, Object> body = body(country("KR",
                entry(1, "2026-09-25T00:00:00.000Z"),
                entry(2, "2026-10-03T00:00:00.000Z")));

        Optional<LocalDate> result = TmdbReleaseDates.koreanTheatricalDate(body, TODAY);

        assertThat(result).contains(LocalDate.of(2026, 10, 3));
    }

    @Test
    void 오늘_이후_type3가_없고_과거만_있으면_type2로_폴백한다() {
        Map<String, Object> body = body(country("KR",
                entry(3, "2019-05-30T00:00:00.000Z"),
                entry(2, "2026-10-03T00:00:00.000Z")));

        assertThat(TmdbReleaseDates.koreanTheatricalDate(body, TODAY)).contains(LocalDate.of(2026, 10, 3));
    }

    @Test
    void KR_항목이_없으면_empty() {
        Map<String, Object> body = body(country("US", entry(3, "2026-09-20T00:00:00.000Z")));

        assertThat(TmdbReleaseDates.koreanTheatricalDate(body, TODAY)).isEmpty();
    }

    @Test
    void 잘못된_날짜_문자열은_건너뛴다() {
        Map<String, Object> body = body(country("KR",
                entry(3, "not-a-date"),
                entry(3, "2026-1"),
                entry(3, null)));

        assertThat(TmdbReleaseDates.koreanTheatricalDate(body, TODAY)).isEmpty();
    }

    @Test
    void 본문이_null_이거나_results가_없으면_empty() {
        assertThat(TmdbReleaseDates.koreanTheatricalDate(null, TODAY)).isEmpty();
        assertThat(TmdbReleaseDates.koreanTheatricalDate(Map.of("id", 1), TODAY)).isEmpty();
    }

    @SafeVarargs
    private static Map<String, Object> body(Map<String, Object>... countries) {
        return Map.of("id", 1, "results", List.of(countries));
    }

    @SafeVarargs
    private static Map<String, Object> country(String iso, Map<String, Object>... dates) {
        return Map.of("iso_3166_1", iso, "release_dates", List.of(dates));
    }

    private static Map<String, Object> entry(int type, String releaseDate) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("type", type);
        m.put("release_date", releaseDate);
        return m;
    }
}
