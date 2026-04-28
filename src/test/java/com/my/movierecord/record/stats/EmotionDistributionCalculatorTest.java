package com.my.movierecord.record.stats;

import com.my.movierecord.record.enums.Emotion;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmotionDistributionCalculatorTest {

    private final EmotionDistributionCalculator calculator = new EmotionDistributionCalculator();

    @Test
    @DisplayName("빈 맵이면 빈 리스트 반환")
    void calculate_emptyMap_returnsEmpty() {
        assertThat(calculator.calculate(Map.of())).isEmpty();
    }

    @Test
    @DisplayName("감정 1개면 100%, offset=0")
    void calculate_singleEmotion_returns100Percent() {
        var result = calculator.calculate(Map.of(Emotion.FUNNY, 5L));

        assertThat(result).hasSize(1);
        EmotionSegment seg = result.get(0);
        assertThat(seg.code()).isEqualTo("funny");
        assertThat(seg.count()).isEqualTo(5L);
        assertThat(seg.percent()).isEqualTo(100);
        assertThat(seg.offset()).isEqualTo(0);
    }

    @Test
    @DisplayName("퍼센트 합이 항상 100")
    void calculate_percentSumAlways100() {
        var result = calculator.calculate(Map.of(
                Emotion.FUNNY, 3L,
                Emotion.SAD, 2L,
                Emotion.TENSE, 1L));

        int sum = result.stream().mapToInt(EmotionSegment::percent).sum();
        assertThat(sum).isEqualTo(100);
    }

    @Test
    @DisplayName("건수 동점은 enum 이름 알파벳 오름차순으로 정렬")
    void calculate_sortsByCountDescThenNameAsc() {
        var result = calculator.calculate(Map.of(
                Emotion.TENSE, 2L,
                Emotion.FUNNY, 2L,
                Emotion.SAD, 1L));

        // FUNNY and TENSE tie at 2; FUNNY < TENSE alphabetically
        assertThat(result.get(0).code()).isEqualTo("funny");
        assertThat(result.get(1).code()).isEqualTo("tense");
        assertThat(result.get(2).code()).isEqualTo("sad");
    }

    @Test
    @DisplayName("offset이 누적되어 다음 세그먼트 시작점이 됨")
    void calculate_offsetAccumulates() {
        // FUNNY=6(60%), SAD=3(30%), NONE=1(last→10%)
        var result = calculator.calculate(Map.of(
                Emotion.FUNNY, 6L,
                Emotion.SAD, 3L,
                Emotion.NONE, 1L));

        assertThat(result.get(0).code()).isEqualTo("funny");
        assertThat(result.get(0).offset()).isEqualTo(0);
        assertThat(result.get(1).code()).isEqualTo("sad");
        assertThat(result.get(1).offset()).isEqualTo(60);
        assertThat(result.get(2).code()).isEqualTo("none");
        assertThat(result.get(2).offset()).isEqualTo(90);
    }
}
