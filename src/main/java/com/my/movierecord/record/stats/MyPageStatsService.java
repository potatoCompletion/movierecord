package com.my.movierecord.record.stats;

import com.my.movierecord.record.enums.Emotion;
import com.my.movierecord.record.enums.Taste;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MyPageStatsService {

    private final WatchRecordStatsRepository statsRepository;
    private final EmotionDistributionCalculator emotionCalculator;
    private final Clock clock;

    public MyPageStatsService(WatchRecordStatsRepository statsRepository,
                              EmotionDistributionCalculator emotionCalculator,
                              Clock clock) {
        this.statsRepository = statsRepository;
        this.emotionCalculator = emotionCalculator;
        this.clock = clock;
    }

    public MyPageStats getStats(Long userId) {
        LocalDate today = LocalDate.now(clock);
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        long totalCount = statsRepository.countByUserId(userId);
        long yearlyCount = statsRepository.countByUserIdAndYear(userId, currentYear);
        long monthlyCount = statsRepository.countByUserIdAndYearAndMonth(userId, currentYear, currentMonth);

        BigDecimal averageRating = BigDecimal.ZERO;
        if (totalCount > 0) {
            BigDecimal raw = statsRepository.averageRatingByUserId(userId);
            if (raw != null) {
                averageRating = raw.setScale(1, RoundingMode.HALF_UP);
            }
        }

        Double tasteMatchRate = null;
        if (totalCount > 0) {
            long matchCount = statsRepository.countByUserIdAndTaste(userId, Taste.MATCH);
            tasteMatchRate = (double) matchCount / totalCount;
        }

        List<MonthlyPoint> monthlyCounts = buildMonthlyCounts(userId, today);
        long maxMonthlyCount = monthlyCounts.stream()
                .mapToLong(MonthlyPoint::count)
                .max()
                .orElse(0L);

        Map<Emotion, Long> rawEmotionCounts = buildEmotionMap(userId);
        List<EmotionSegment> emotionSegments = emotionCalculator.calculate(rawEmotionCounts);

        return new MyPageStats(
                totalCount, yearlyCount, monthlyCount,
                averageRating, tasteMatchRate,
                monthlyCounts, maxMonthlyCount,
                emotionSegments);
    }

    private List<MonthlyPoint> buildMonthlyCounts(Long userId, LocalDate today) {
        YearMonth currentYM = YearMonth.from(today);
        YearMonth startYM = currentYM.minusMonths(11);
        LocalDate from = startYM.atDay(1);
        LocalDate to = currentYM.atEndOfMonth();

        List<Object[]> rows = statsRepository.countMonthlyByUserIdAndDateRange(userId, from, to);
        Map<YearMonth, Long> countByMonth = new HashMap<>();
        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            countByMonth.put(YearMonth.of(year, month), count);
        }

        List<MonthlyPoint> points = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth ym = startYM.plusMonths(i);
            points.add(new MonthlyPoint(ym, countByMonth.getOrDefault(ym, 0L)));
        }
        return points;
    }

    private Map<Emotion, Long> buildEmotionMap(Long userId) {
        List<Object[]> rows = statsRepository.countByUserIdGroupByEmotion(userId);
        Map<Emotion, Long> map = new EnumMap<>(Emotion.class);
        for (Object[] row : rows) {
            Emotion emotion = (Emotion) row[0];
            long count = ((Number) row[1]).longValue();
            map.put(emotion, count);
        }
        return map;
    }
}
