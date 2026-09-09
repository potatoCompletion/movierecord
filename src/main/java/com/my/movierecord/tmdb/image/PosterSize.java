package com.my.movierecord.tmdb.image;

/**
 * TMDB 이미지 CDN 경로 세그먼트({@code /t/p/{size}/…})에 들어가는 사이즈 값.
 *
 * <p>현재 코드에서 실제로 사용하는 사이즈만 정의한다. 새 사이즈가 필요하면 여기에 추가한다.
 */
public enum PosterSize {
    W185("w185"),
    W342("w342"),
    W500("w500"),
    W1280("w1280");

    private final String value;

    PosterSize(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
