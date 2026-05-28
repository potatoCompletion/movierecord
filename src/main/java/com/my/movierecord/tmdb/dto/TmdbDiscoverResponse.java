package com.my.movierecord.tmdb.dto;

import java.util.List;

/**
 * TMDB /discover/movie 응답 wrapper.
 * results 목록과 함께 total_pages 메타데이터를 보존한다.
 */
public record TmdbDiscoverResponse(List<TmdbDiscoverItem> results, int totalPages) {
}
