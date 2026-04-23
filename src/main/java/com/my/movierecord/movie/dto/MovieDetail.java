package com.my.movierecord.movie.dto;

import com.my.movierecord.movie.domain.Movie;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovieDetail(
        Long id,
        String title,
        LocalDate watchedDate,
        String thumbnailUrl,
        String oneLiner,
        String immersion,
        String story,
        String emotion,
        String goodPoints,
        String badPoints,
        String taste,
        BigDecimal rating
) {
    public static MovieDetail from(Movie movie) {
        String thumbnailUrl = movie.getThumbnailPath() == null ? null : "/uploads/" + movie.getThumbnailPath();
        return new MovieDetail(
                movie.getId(),
                movie.getTitle(),
                movie.getWatchedDate(),
                thumbnailUrl,
                movie.getOneLiner(),
                movie.getImmersion().getDisplayName(),
                movie.getStory().getDisplayName(),
                movie.getEmotion().getDisplayName(),
                movie.getGoodPoints(),
                movie.getBadPoints(),
                movie.getTaste().getDisplayName(),
                movie.getRating()
        );
    }
}
