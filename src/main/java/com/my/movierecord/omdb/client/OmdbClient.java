package com.my.movierecord.omdb.client;

import com.my.movierecord.omdb.config.OmdbProperties;
import com.my.movierecord.omdb.dto.OmdbRating;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OmdbClient {

    private final RestClient omdbRestClient;
    private final OmdbProperties omdbProperties;

    public List<OmdbRating> getRatings(String imdbId) {
        if (imdbId == null || imdbId.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> raw = omdbRestClient.get()
                    .uri(b -> b.queryParam("i", imdbId).queryParam("apikey", omdbProperties.key()).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (raw == null || !"True".equals(raw.get("Response"))) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ratings = (List<Map<String, Object>>) raw.get("Ratings");
            if (ratings == null) {
                return List.of();
            }

            return ratings.stream()
                    .map(r -> {
                        String normalized = normalizeSource((String) r.get("Source"));
                        String value = (String) r.get("Value");
                        return new OmdbRating(normalized, value, computeCssClass(normalized, value));
                    })
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String normalizeSource(String source) {
        return switch (source) {
            case "Internet Movie Database" -> "IMDb";
            default -> source;
        };
    }

    private static String computeCssClass(String source, String value) {
        return switch (source) {
            case "IMDb" -> "rating-imdb";
            case "Rotten Tomatoes" -> {
                try {
                    int score = Integer.parseInt(value.replace("%", "").trim());
                    yield score >= 60 ? "rating-rt-fresh" : "rating-rt-rotten";
                } catch (NumberFormatException e) {
                    yield "rating-rt-fresh";
                }
            }
            case "Metacritic" -> {
                try {
                    int score = Integer.parseInt(value.split("/")[0].trim());
                    if (score >= 75) yield "rating-mc-green";
                    else if (score >= 50) yield "rating-mc-yellow";
                    else yield "rating-mc-red";
                } catch (NumberFormatException e) {
                    yield "rating-mc-green";
                }
            }
            default -> "";
        };
    }
}
