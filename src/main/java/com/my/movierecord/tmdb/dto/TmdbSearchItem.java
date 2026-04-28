package com.my.movierecord.tmdb.dto;

public record TmdbSearchItem(
        Long id,
        String title,
        String posterPath,
        String mediaType,
        String releaseDate
) {}
