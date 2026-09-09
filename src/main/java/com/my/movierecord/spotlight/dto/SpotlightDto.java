package com.my.movierecord.spotlight.dto;

import com.my.movierecord.spotlight.domain.SpotlightHistory;
import com.my.movierecord.tmdb.image.PosterSize;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;

public record SpotlightDto(
        Long tmdbId,
        String title,
        String originalTitle,
        String posterPath,
        String backdropPath,
        String posterUrl,     // 완성 URL (w500), 경로 없으면 null
        String backdropUrl,   // 완성 URL (w1280), 경로 없으면 null
        String releaseYear,
        String overview,
        Double tmdbRating,
        String rtScore
) {
    public static SpotlightDto from(SpotlightHistory h, TmdbImageUrlProvider images) {
        return new SpotlightDto(
                h.getTmdbId(),
                h.getTitle(),
                h.getOriginalTitle(),
                h.getPosterPath(),
                h.getBackdropPath(),
                images.poster(h.getPosterPath(), PosterSize.W500),
                images.backdrop(h.getBackdropPath(), PosterSize.W1280),
                h.getReleaseYear(),
                h.getOverview(),
                h.getTmdbRating(),
                h.getRtScore()
        );
    }
}
