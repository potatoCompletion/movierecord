package com.my.movierecord.tmdb.client;

import com.my.movierecord.tmdb.dto.TmdbSearchItem;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TmdbClient {

    private static final String SEARCH_PATH = "/search/multi";

    private final RestClient restClient;

    public TmdbClient(@Qualifier("tmdbRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<TmdbSearchItem> search(String query) {
        Map<String, Object> body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_PATH)
                        .queryParam("query", query)
                        .queryParam("include_adult", false)
                        .queryParam("language", "ko-KR")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (body == null) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        if (results == null) {
            return List.of();
        }

        return results.stream()
                .filter(r -> !"person".equals(r.get("media_type")))
                .map(this::toSearchItem)
                .toList();
    }

    private TmdbSearchItem toSearchItem(Map<String, Object> raw) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        String mediaType = (String) raw.get("media_type");
        boolean isTv = "tv".equals(mediaType);
        String title = isTv ? (String) raw.get("name") : (String) raw.get("title");
        String posterPath = (String) raw.get("poster_path");
        String releaseDate = isTv ? (String) raw.get("first_air_date") : (String) raw.get("release_date");
        return new TmdbSearchItem(id, title, posterPath, mediaType, releaseDate);
    }
}
