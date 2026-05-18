package com.my.movierecord.tmdb.dto;

import java.util.List;
import java.util.Map;

public record TmdbMovieDetail(
        Long id,
        String title,
        String originalTitle,
        String tagline,
        String overview,
        String posterPath,
        String backdropPath,
        String releaseDate,
        Integer runtime,
        List<TmdbGenreItem> genres,
        Double voteAverage,
        Integer voteCount,
        String originalLanguage,
        List<String> productionCountries
) {
    @SuppressWarnings("unchecked")
    public static TmdbMovieDetail from(Map<String, Object> raw) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        String title = (String) raw.get("title");
        String originalTitle = (String) raw.get("original_title");
        String tagline = (String) raw.get("tagline");
        String overview = (String) raw.get("overview");
        String posterPath = (String) raw.get("poster_path");
        String backdropPath = (String) raw.get("backdrop_path");
        String releaseDate = (String) raw.get("release_date");
        Integer runtime = raw.get("runtime") instanceof Number n ? n.intValue() : null;

        List<Map<String, Object>> rawGenres =
                (List<Map<String, Object>>) raw.getOrDefault("genres", List.of());
        List<TmdbGenreItem> genres = rawGenres.stream()
                .map(g -> new TmdbGenreItem(
                        g.get("id") instanceof Number n ? n.longValue() : null,
                        (String) g.get("name")))
                .toList();

        Double voteAverage = raw.get("vote_average") instanceof Number n ? n.doubleValue() : null;
        Integer voteCount = raw.get("vote_count") instanceof Number n ? n.intValue() : null;
        String originalLanguage = (String) raw.get("original_language");

        List<Map<String, Object>> rawCountries =
                (List<Map<String, Object>>) raw.getOrDefault("production_countries", List.of());
        List<String> productionCountries = rawCountries.stream()
                .map(c -> (String) c.get("name"))
                .toList();

        return new TmdbMovieDetail(id, title, originalTitle, tagline, overview, posterPath,
                backdropPath, releaseDate, runtime, genres, voteAverage, voteCount,
                originalLanguage, productionCountries);
    }
}
