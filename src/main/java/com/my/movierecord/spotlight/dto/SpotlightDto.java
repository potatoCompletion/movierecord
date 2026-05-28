package com.my.movierecord.spotlight.dto;

import com.my.movierecord.spotlight.domain.SpotlightHistory;

public record SpotlightDto(
        Long tmdbId,
        String title,
        String originalTitle,
        String posterPath,
        String backdropPath,
        String releaseYear,
        String overview,
        Double tmdbRating,
        String rtScore
) {
    public static SpotlightDto from(SpotlightHistory h) {
        return new SpotlightDto(
                h.getTmdbId(),
                h.getTitle(),
                h.getOriginalTitle(),
                h.getPosterPath(),
                h.getBackdropPath(),
                h.getReleaseYear(),
                h.getOverview(),
                h.getTmdbRating(),
                h.getRtScore()
        );
    }
}
