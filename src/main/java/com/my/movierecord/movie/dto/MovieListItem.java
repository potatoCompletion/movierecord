package com.my.movierecord.movie.dto;

import com.my.movierecord.movie.domain.Movie;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovieListItem(
        Long id,
        String title,
        LocalDate watchedDate,
        String thumbnailUrl,
        BigDecimal rating,
        MovieDetail detail
) {
    public static MovieListItem from(Movie movie) {
        String thumbnailUrl = movie.getThumbnailPath() == null ? null : "/uploads/" + movie.getThumbnailPath();
        return new MovieListItem(
                movie.getId(),
                movie.getTitle(),
                movie.getWatchedDate(),
                thumbnailUrl,
                movie.getRating(),
                MovieDetail.from(movie)
        );
    }
}
