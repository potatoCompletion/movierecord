package com.my.movierecord.tmdb.dto;

import com.my.movierecord.tmdb.image.PosterSize;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;
import java.util.List;
import java.util.Map;

public record TmdbTvDetail(
        Long id,
        String name,
        String originalName,
        String overview,
        String posterPath,
        String backdropPath,
        String posterUrl,     // 완성 URL (w342), 경로 없으면 null
        String backdropUrl,   // 완성 URL (w1280), 경로 없으면 null
        String firstAirDate,
        Integer numberOfSeasons,
        Integer numberOfEpisodes,
        List<TmdbGenreItem> genres,
        Double voteAverage,
        Integer voteCount
) {
    @SuppressWarnings("unchecked")
    public static TmdbTvDetail from(Map<String, Object> raw, TmdbImageUrlProvider images) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        String name = (String) raw.get("name");
        String originalName = (String) raw.get("original_name");
        String overview = (String) raw.get("overview");
        String posterPath = (String) raw.get("poster_path");
        String backdropPath = (String) raw.get("backdrop_path");
        String firstAirDate = (String) raw.get("first_air_date");
        Integer numberOfSeasons = raw.get("number_of_seasons") instanceof Number n ? n.intValue() : null;
        Integer numberOfEpisodes = raw.get("number_of_episodes") instanceof Number n ? n.intValue() : null;

        List<Map<String, Object>> rawGenres =
                (List<Map<String, Object>>) raw.getOrDefault("genres", List.of());
        List<TmdbGenreItem> genres = rawGenres.stream()
                .map(g -> new TmdbGenreItem(
                        g.get("id") instanceof Number n ? n.longValue() : null,
                        (String) g.get("name")))
                .toList();

        Double voteAverage = raw.get("vote_average") instanceof Number n ? n.doubleValue() : null;
        Integer voteCount = raw.get("vote_count") instanceof Number n ? n.intValue() : null;

        return new TmdbTvDetail(id, name, originalName, overview, posterPath, backdropPath,
                images.poster(posterPath, PosterSize.W342),
                images.backdrop(backdropPath, PosterSize.W1280),
                firstAirDate, numberOfSeasons, numberOfEpisodes, genres, voteAverage, voteCount);
    }
}
