package com.my.movierecord.record.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EmotionTest {

    @Test
    @DisplayName("LINGERING은 'linger' 코드를 반환")
    void getCode_lingering_returnsLinger() {
        assertThat(Emotion.LINGERING.getCode()).isEqualTo("linger");
    }

    @ParameterizedTest
    @CsvSource({
        "FUNNY, funny",
        "TENSE, tense",
        "SAD,   sad",
        "NONE,  none"
    })
    @DisplayName("LINGERING 외 감정은 이름 소문자를 코드로 반환")
    void getCode_nonLingering_returnsLowercaseName(Emotion emotion, String expected) {
        assertThat(emotion.getCode()).isEqualTo(expected);
    }
}
