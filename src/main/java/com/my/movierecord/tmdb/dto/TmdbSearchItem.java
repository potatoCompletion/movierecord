package com.my.movierecord.tmdb.dto;

public record TmdbSearchItem(
        Long id,
        String title,
        String posterPath,
        String posterUrl,   // 검색 결과 페이지용 완성 URL (w185), 경로 없으면 null
        String mediaType,
        String releaseDate
) {}
