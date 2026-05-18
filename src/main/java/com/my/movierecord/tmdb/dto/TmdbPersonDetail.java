package com.my.movierecord.tmdb.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TmdbPersonDetail(
        Long id,
        String name,
        String profilePath,
        String biography,
        String birthday,
        String knownForDepartment,
        List<FilmographyItem> filmography
) {
    public record FilmographyItem(
            Long tmdbId,
            String mediaType,
            String title,
            String year,
            String posterPath
    ) {}

    @SuppressWarnings("unchecked")
    public static TmdbPersonDetail from(Map<String, Object> personRaw, Map<String, Object> creditsRaw) {
        Long id = personRaw.get("id") instanceof Number n ? n.longValue() : null;
        String name = (String) personRaw.get("name");
        String profilePath = (String) personRaw.get("profile_path");
        String biography = (String) personRaw.get("biography");
        String birthday = (String) personRaw.get("birthday");
        String knownForDepartment = (String) personRaw.get("known_for_department");

        return new TmdbPersonDetail(id, name, profilePath, biography, birthday,
                knownForDepartment, buildFilmography(creditsRaw));
    }

    @SuppressWarnings("unchecked")
    private static List<FilmographyItem> buildFilmography(Map<String, Object> creditsRaw) {
        if (creditsRaw == null) return List.of();

        List<Map<String, Object>> cast =
                (List<Map<String, Object>>) creditsRaw.getOrDefault("cast", List.of());
        List<Map<String, Object>> crew =
                (List<Map<String, Object>>) creditsRaw.getOrDefault("crew", List.of());

        Map<Long, FilmographyItem> seen = new LinkedHashMap<>();
        for (Map<String, Object> item : cast) addItem(item, seen);
        for (Map<String, Object> item : crew) addItem(item, seen);

        List<FilmographyItem> result = new ArrayList<>(seen.values());
        result.sort((a, b) -> {
            if (a.year() == null && b.year() == null) return 0;
            if (a.year() == null) return 1;
            if (b.year() == null) return -1;
            return b.year().compareTo(a.year());
        });
        return result;
    }

    private static void addItem(Map<String, Object> raw, Map<Long, FilmographyItem> seen) {
        if (!(raw.get("id") instanceof Number)) return;
        Long tmdbId = ((Number) raw.get("id")).longValue();
        if (seen.containsKey(tmdbId)) return;

        String mediaType = (String) raw.get("media_type");
        boolean isTv = "tv".equals(mediaType);
        String title = isTv ? (String) raw.get("name") : (String) raw.get("title");
        String dateStr = isTv ? (String) raw.get("first_air_date") : (String) raw.get("release_date");
        String year = (dateStr != null && dateStr.length() >= 4) ? dateStr.substring(0, 4) : null;
        String posterPath = (String) raw.get("poster_path");

        seen.put(tmdbId, new FilmographyItem(tmdbId, mediaType, title, year, posterPath));
    }
}
