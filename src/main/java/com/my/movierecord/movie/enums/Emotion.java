package com.my.movierecord.movie.enums;

/**
 * 영화 감상 시 느낀 감정 반응 enum.
 * 각 enum 상수는 한글 displayName을 가지고 있어 UI에서 직접 표시할 수 있다.
 */
public enum Emotion {
    FUNNY("웃김"),        // 웃음, 유머가 있었던 영화
    TENSE("긴장"),       // 긴장감 있었던 영화
    SAD("슬픔"),         // 슬픔을 느꼈던 영화
    LINGERING("여운"),   // 여운이 남았던 영화
    NONE("없음");        // 특별한 감정 반응이 없음

    private final String displayName;

    Emotion(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 클라이언트에 표시할 한글 문자열을 반환한다.
     */
    public String getDisplayName() {
        return displayName;
    }
}
