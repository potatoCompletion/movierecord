package com.my.movierecord.record.enums;

public enum Immersion {
    GOOD("좋음"),
    NORMAL("보통"),
    BAD("별로");

    private final String displayName;

    Immersion(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
