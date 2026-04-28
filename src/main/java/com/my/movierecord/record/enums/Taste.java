package com.my.movierecord.record.enums;

public enum Taste {
    MATCH("맞음"),
    MISMATCH("안맞음");

    private final String displayName;

    Taste(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
