package com.my.movierecord.tmdb.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public record UpcomingItem(
        Long id,
        String title,
        String originalTitle,
        String posterPath,
        LocalDate releaseDate,
        long ddays           // ChronoUnit.DAYS.between(today, releaseDate)
) {
    public static UpcomingItem from(Map<String, Object> raw, LocalDate today) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        LocalDate release = LocalDate.parse((String) raw.get("release_date"));
        return new UpcomingItem(
                id,
                (String) raw.get("title"),
                (String) raw.get("original_title"),
                (String) raw.get("poster_path"),
                release,
                ChronoUnit.DAYS.between(today, release)
        );
    }
}
