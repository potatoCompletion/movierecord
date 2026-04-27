package com.my.movierecord.movie.enums;

/**
 * 영화의 스토리/플롯 평가 enum.
 * 영화의 내용이 얼마나 설득력 있고 흥미로웠는지를 나타낸다.
 * 각 enum 상수는 한글 displayName을 가지고 있어 UI에서 직접 표시할 수 있다.
 */
public enum Story {
    CONVINCING("납득됨"),  // 스토리가 설득력 있고 잘 짜여짐
    SO_SO("그냥저냥"),     // 스토리가 평이함
    BAD("별로");           // 스토리가 마음에 들지 않음

    private final String displayName;

    Story(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 클라이언트에 표시할 한글 문자열을 반환한다.
     */
    public String getDisplayName() {
        return displayName;
    }
}
