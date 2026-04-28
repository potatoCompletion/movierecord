package com.my.movierecord.record.stats;

import com.my.movierecord.record.enums.Taste;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MyPageStatsServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDate.of(2025, 6, 15).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));

    @Mock
    WatchRecordStatsRepository statsRepository;

    @Mock
    EmotionDistributionCalculator emotionCalculator;

    private MyPageStatsService service;

    @BeforeEach
    void setUp() {
        service = new MyPageStatsService(statsRepository, emotionCalculator, FIXED_CLOCK);
    }

    @Test
    @DisplayName("기록 없으면 기본값 반환 및 12개월 윈도우 구성")
    void getStats_noRecords_returnsDefaultsAndTwelveMonthWindow() {
        given(statsRepository.countByUserId(1L)).willReturn(0L);
        given(statsRepository.countByUserIdAndYear(1L, 2025)).willReturn(0L);
        given(statsRepository.countByUserIdAndYearAndMonth(1L, 2025, 6)).willReturn(0L);
        given(statsRepository.countMonthlyByUserIdAndDateRange(
                eq(1L), any(LocalDate.class), any(LocalDate.class))).willReturn(List.of());
        given(statsRepository.countByUserIdGroupByEmotion(1L)).willReturn(List.of());
        given(emotionCalculator.calculate(any())).willReturn(List.of());

        MyPageStats stats = service.getStats(1L);

        assertThat(stats.totalCount()).isZero();
        assertThat(stats.averageRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.tasteMatchRate()).isNull();
        assertThat(stats.emotionSegments()).isEmpty();
        assertThat(stats.monthlyCounts()).hasSize(12);
        assertThat(stats.monthlyCounts().get(0).yearMonth()).isEqualTo(YearMonth.of(2024, 7));
        assertThat(stats.monthlyCounts().get(11).yearMonth()).isEqualTo(YearMonth.of(2025, 6));
    }

    @Test
    @DisplayName("평균 평점이 소수점 1자리로 반올림되고 취향 적중률이 계산됨")
    void getStats_withRecords_roundsAverageRatingAndComputesTasteMatch() {
        given(statsRepository.countByUserId(1L)).willReturn(5L);
        given(statsRepository.countByUserIdAndYear(1L, 2025)).willReturn(3L);
        given(statsRepository.countByUserIdAndYearAndMonth(1L, 2025, 6)).willReturn(1L);
        given(statsRepository.averageRatingByUserId(1L)).willReturn(new BigDecimal("4.25"));
        given(statsRepository.countByUserIdAndTaste(1L, Taste.MATCH)).willReturn(3L);
        given(statsRepository.countMonthlyByUserIdAndDateRange(
                eq(1L), any(LocalDate.class), any(LocalDate.class))).willReturn(List.of());
        given(statsRepository.countByUserIdGroupByEmotion(1L)).willReturn(List.of());
        given(emotionCalculator.calculate(any())).willReturn(List.of());

        MyPageStats stats = service.getStats(1L);

        assertThat(stats.averageRating()).isEqualByComparingTo(new BigDecimal("4.3"));
        assertThat(stats.tasteMatchRate()).isEqualTo(3.0 / 5.0);
    }
}
