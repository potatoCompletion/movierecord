package com.my.movierecord.movie.enums;

/**
 * 영화와 개인 취향의 일치도 enum.
 * 감상한 영화가 자신의 취향과 맞는지를 나타낸다.
 * 각 enum 상수는 한글 displayName을 가지고 있어 UI에서 직접 표시할 수 있다.
 */
public enum Taste {
    MATCH("맞음"),        // 영화가 자신의 취향과 잘 맞음
    MISMATCH("안맞음");   // 영화가 자신의 취향과 맞지 않음

    private final String displayName;

    Taste(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 클라이언트에 표시할 한글 문자열을 반환한다.
     */
    public String getDisplayName() {
        return displayName;
    }
}
