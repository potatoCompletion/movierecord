package com.my.movierecord.tmdb.dto;

import com.my.movierecord.tmdb.image.PosterSize;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;
import java.util.Map;

public record NowPlayingItem(
        Long id,
        String title,
        String originalTitle,
        String posterPath,  // raw TMDB path: "/abc.jpg"
        String posterUrl    // 뷰에서 바로 쓰는 완성 URL (w500), 경로 없으면 null
) {
    public static NowPlayingItem from(Map<String, Object> raw, TmdbImageUrlProvider images) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        String posterPath = (String) raw.get("poster_path");
        return new NowPlayingItem(
                id,
                (String) raw.get("title"),
                (String) raw.get("original_title"),
                posterPath,
                images.poster(posterPath, PosterSize.W500)
        );
    }
}
