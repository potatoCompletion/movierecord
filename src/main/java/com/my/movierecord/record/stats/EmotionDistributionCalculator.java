package com.my.movierecord.record.stats;

import com.my.movierecord.record.enums.Emotion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EmotionDistributionCalculator {

    public List<EmotionSegment> calculate(Map<Emotion, Long> rawCounts) {
        if (rawCounts.isEmpty()) {
            return List.of();
        }

        long total = rawCounts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return List.of();
        }

        List<Map.Entry<Emotion, Long>> sorted = rawCounts.entrySet().stream()
                .sorted(Comparator
                        .<Map.Entry<Emotion, Long>, Long>comparing(Map.Entry::getValue).reversed()
                        .thenComparing(e -> e.getKey().name()))
                .toList();

        List<EmotionSegment> segments = new ArrayList<>();
        int offset = 0;
        int percentSum = 0;

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<Emotion, Long> entry = sorted.get(i);
            Emotion emotion = entry.getKey();
            long count = entry.getValue();

            int percent;
            if (i == sorted.size() - 1) {
                percent = 100 - percentSum;
            } else {
                percent = (int) (count * 100 / total);
            }

            segments.add(new EmotionSegment(
                    emotion.getCode(),
                    emotion.getDisplayName(),
                    count,
                    percent,
                    offset));

            percentSum += percent;
            offset += percent;
        }

        return segments;
    }
}
