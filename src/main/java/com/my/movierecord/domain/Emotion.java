package com.my.movierecord.domain;

public enum Emotion {
    FUNNY("웃김"),
    TENSE("긴장"),
    SAD("슬픔"),
    LINGERING("여운"),
    NONE("없음");

    private final String displayName;

    Emotion(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
