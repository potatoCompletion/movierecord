package com.my.movierecord.tmdb.dto;

import java.util.Map;

public record NowPlayingItem(
        Long id,
        String title,
        String originalTitle,
        String posterPath   // raw TMDB path: "/abc.jpg" — 뷰에서 CDN 프리픽스 붙임
) {
    public static NowPlayingItem from(Map<String, Object> raw) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        return new NowPlayingItem(
                id,
                (String) raw.get("title"),
                (String) raw.get("original_title"),
                (String) raw.get("poster_path")
        );
    }
}
