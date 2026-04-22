package com.my.movierecord.domain;

public enum Story {
    CONVINCING("납득됨"),
    SO_SO("그냥저냥"),
    BAD("별로");

    private final String displayName;

    Story(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
