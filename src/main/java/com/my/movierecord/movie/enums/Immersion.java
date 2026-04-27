package com.my.movierecord.movie.enums;

/**
 * 영화 감상 시의 몰입감 정도 enum.
 * 영화에 얼마나 집중해서 감상했는지를 나타낸다.
 * 각 enum 상수는 한글 displayName을 가지고 있어 UI에서 직접 표시할 수 있다.
 */
public enum Immersion {
    GOOD("좋음"),        // 영화에 매우 몰입했음
    NORMAL("보통"),      // 보통 정도의 몰입감
    BAD("별로");         // 몰입감이 떨어짐

    private final String displayName;

    Immersion(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 클라이언트에 표시할 한글 문자열을 반환한다.
     */
    public String getDisplayName() {
        return displayName;
    }
}
