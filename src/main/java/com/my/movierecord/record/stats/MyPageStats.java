package com.my.movierecord.record.stats;

import java.math.BigDecimal;
import java.util.List;

public record MyPageStats(
        long totalCount,
        long yearlyCount,
        long monthlyCount,
        BigDecimal averageRating,
        Double tasteMatchRate,
        List<MonthlyPoint> monthlyCounts,
        long maxMonthlyCount,
        List<EmotionSegment> emotionSegments) {}
