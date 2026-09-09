package com.my.movierecord.tmdb.dto;

import com.my.movierecord.tmdb.image.PosterSize;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public record UpcomingItem(
        Long id,
        String title,
        String originalTitle,
        String posterPath,
        String posterUrl,        // 뷰에서 바로 쓰는 완성 URL (w500), 경로 없으면 null
        String releaseDateText,  // pre-formatted "MM.dd" — avoids LocalDate cache deserialization issues
        long ddays               // ChronoUnit.DAYS.between(today, releaseDate)
) {
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("MM.dd");

    public static UpcomingItem from(Map<String, Object> raw, LocalDate today, TmdbImageUrlProvider images) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        LocalDate release = LocalDate.parse((String) raw.get("release_date"));
        String posterPath = (String) raw.get("poster_path");
        return new UpcomingItem(
                id,
                (String) raw.get("title"),
                (String) raw.get("original_title"),
                posterPath,
                images.poster(posterPath, PosterSize.W500),
                release.format(DISPLAY_FMT),
                ChronoUnit.DAYS.between(today, release)
        );
    }
}
