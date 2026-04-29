package com.my.movierecord.record.enums;

public enum Emotion {
    FUNNY("funny", "웃김"),
    TENSE("tense", "긴장"),
    SAD("sad", "슬픔"),
    LINGERING("linger", "여운"),
    SCARY("scary", "공포"),
    NONE("none", "없음");

    private final String code;
    private final String displayName;

    Emotion(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
