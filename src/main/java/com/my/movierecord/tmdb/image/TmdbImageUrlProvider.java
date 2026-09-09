package com.my.movierecord.tmdb.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TMDB 이미지 CDN URL 조립을 한곳에 모은 헬퍼.
 *
 * <p>{@code tmdb.api.image-base-url}(사이즈를 포함하지 않는 CDN 루트, 예: {@code https://image.tmdb.org/t/p})에
 * 사이즈 세그먼트와 TMDB 가 내려주는 경로({@code /abc.jpg})를 이어 붙인다.
 * 경로가 null 이거나 비어 있으면 null 을 돌려주므로 호출 측은 기존처럼 null 가드만 하면 된다.
 */
@Component
public class TmdbImageUrlProvider {

    private final String baseUrl;

    public TmdbImageUrlProvider(@Value("${tmdb.api.image-base-url}") String baseUrl) {
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    /** 포스터 URL. posterPath 가 null/blank 면 null. */
    public String poster(String posterPath, PosterSize size) {
        return build(posterPath, size);
    }

    /** 배경(backdrop) URL. 조립 규칙은 포스터와 같다. */
    public String backdrop(String backdropPath, PosterSize size) {
        return build(backdropPath, size);
    }

    /** 인물 프로필 URL. 조립 규칙은 포스터와 같다. */
    public String profile(String profilePath, PosterSize size) {
        return build(profilePath, size);
    }

    private String build(String path, PosterSize size) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return baseUrl + "/" + size.value() + "/" + stripLeadingSlash(path);
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String stripLeadingSlash(String s) {
        return s.startsWith("/") ? s.substring(1) : s;
    }
}
