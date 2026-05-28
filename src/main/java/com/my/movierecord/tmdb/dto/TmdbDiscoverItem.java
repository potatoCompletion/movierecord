package com.my.movierecord.tmdb.dto;

import java.util.Map;

public record TmdbDiscoverItem(
        Long id,
        String title,
        String originalTitle,
        String overview,
        String posterPath,
        String backdropPath,
        String releaseDate,
        Double voteAverage,
        Integer voteCount
) {
    public static TmdbDiscoverItem from(Map<String, Object> raw) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        Double voteAverage = raw.get("vote_average") instanceof Number n ? n.doubleValue() : null;
        Integer voteCount = raw.get("vote_count") instanceof Number n ? n.intValue() : null;
        return new TmdbDiscoverItem(
                id,
                (String) raw.get("title"),
                (String) raw.get("original_title"),
                (String) raw.get("overview"),
                (String) raw.get("poster_path"),
                (String) raw.get("backdrop_path"),
                (String) raw.get("release_date"),
                voteAverage,
                voteCount
        );
    }
}
