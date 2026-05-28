package com.my.movierecord.tmdb.client;

import com.my.movierecord.tmdb.dto.NowPlayingItem;
import com.my.movierecord.tmdb.dto.TmdbDiscoverItem;
import com.my.movierecord.tmdb.dto.TmdbDiscoverResponse;
import com.my.movierecord.tmdb.dto.TmdbMovieDetail;
import com.my.movierecord.tmdb.dto.TmdbPersonDetail;
import com.my.movierecord.tmdb.dto.TmdbSearchItem;
import com.my.movierecord.tmdb.dto.TmdbTvDetail;
import com.my.movierecord.tmdb.dto.UpcomingItem;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TmdbClient {

    private static final String SEARCH_MULTI_PATH = "/search/multi";
    private static final String SEARCH_MOVIE_PATH = "/search/movie";
    private static final String MOVIE_PATH = "/movie/{id}";
    private static final String TV_PATH = "/tv/{id}";
    private static final String PERSON_PATH = "/person/{id}";
    private static final String PERSON_CREDITS_PATH = "/person/{id}/combined_credits";
    private static final String TV_EXTERNAL_IDS_PATH = "/tv/{id}/external_ids";
    private static final String NOW_PLAYING_PATH    = "/movie/now_playing";
    private static final String DISCOVER_MOVIE_PATH = "/discover/movie";

    private final RestClient restClient;

    public TmdbClient(@Qualifier("tmdbRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<TmdbSearchItem> searchMulti(String query) {
        Map<String, Object> body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_MULTI_PATH)
                        .queryParam("query", query)
                        .queryParam("include_adult", false)
                        .queryParam("language", "ko-KR")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

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
                .map(this::toMultiSearchItem)
                .toList();
    }

    private TmdbSearchItem toMultiSearchItem(Map<String, Object> raw) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        String mediaType = (String) raw.get("media_type");
        boolean isTv = "tv".equals(mediaType);
        String title = isTv ? (String) raw.get("name") : (String) raw.get("title");
        String posterPath = (String) raw.get("poster_path");
        String releaseDate = isTv ? (String) raw.get("first_air_date") : (String) raw.get("release_date");
        return new TmdbSearchItem(id, title, posterPath, mediaType, releaseDate);
    }

    public List<TmdbSearchItem> searchMovie(String query, String primaryReleaseYear) {
        Map<String, Object> body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_MOVIE_PATH)
                        .queryParam("query", query)
                        .queryParam("language", "ko-KR")
                        .queryParam("primary_release_year", primaryReleaseYear)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (body == null) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        if (results == null) {
            return List.of();
        }

        return results.stream()
                .map(this::toMovieSearchItem)
                .toList();
    }

    private TmdbSearchItem toMovieSearchItem(Map<String, Object> raw) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        String title = (String) raw.get("title");
        String posterPath = (String) raw.get("poster_path");
        String releaseDate = (String) raw.get("release_date");
        return new TmdbSearchItem(id, title, posterPath, "movie", releaseDate);
    }

    public List<TmdbSearchItem> searchMultiUnified(String query) {
        Map<String, Object> body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_MULTI_PATH)
                        .queryParam("query", query)
                        .queryParam("include_adult", false)
                        .queryParam("language", "ko-KR")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (body == null) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        if (results == null) {
            return List.of();
        }

        return results.stream()
                .map(this::toUnifiedSearchItem)
                .toList();
    }

    private TmdbSearchItem toUnifiedSearchItem(Map<String, Object> raw) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        String mediaType = (String) raw.get("media_type");
        boolean isPerson = "person".equals(mediaType);
        boolean isTv = "tv".equals(mediaType);
        String title = (isPerson || isTv) ? (String) raw.get("name") : (String) raw.get("title");
        String posterPath = isPerson ? (String) raw.get("profile_path") : (String) raw.get("poster_path");
        String releaseDate = isPerson ? null
                : (isTv ? (String) raw.get("first_air_date") : (String) raw.get("release_date"));
        return new TmdbSearchItem(id, title, posterPath, mediaType, releaseDate);
    }

    public TmdbMovieDetail getMovieDetail(Long tmdbId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(MOVIE_PATH)
                        .queryParam("language", "ko-KR")
                        .build(tmdbId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return raw != null ? TmdbMovieDetail.from(raw) : null;
    }

    public TmdbTvDetail getTvDetail(Long tmdbId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(TV_PATH)
                        .queryParam("language", "ko-KR")
                        .build(tmdbId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return raw != null ? TmdbTvDetail.from(raw) : null;
    }

    public String getTvExternalIds(Long tmdbId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(TV_EXTERNAL_IDS_PATH)
                        .build(tmdbId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return raw != null ? (String) raw.get("imdb_id") : null;
    }

    @SuppressWarnings("unchecked")
    public List<NowPlayingItem> getNowPlaying() {
        Map<String, Object> body = restClient.get()
                .uri(b -> b.path(NOW_PLAYING_PATH)
                        .queryParam("language", "ko-KR")
                        .queryParam("page", "1")
                        .queryParam("region", "KR")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (body == null) return List.of();
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        return results == null ? List.of() :
                results.stream().map(NowPlayingItem::from).limit(10).toList();
    }

    @SuppressWarnings("unchecked")
    public List<UpcomingItem> getUpcoming() {
        LocalDate today = LocalDate.now();
        Map<String, Object> body = restClient.get()
                .uri(b -> b.path(DISCOVER_MOVIE_PATH)
                        .queryParam("include_adult", "false")
                        .queryParam("include_video", "false")
                        .queryParam("language", "ko-KR")
                        .queryParam("page", "1")
                        .queryParam("region", "KR")
                        .queryParam("release_date.gte", today.plusDays(1).toString())
                        .queryParam("release_date.lte", today.plusDays(21).toString())
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("with_release_type", "3")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (body == null) return List.of();
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        if (results == null) return List.of();

        return results.stream()
                .filter(r -> r.get("release_date") instanceof String s && !s.isBlank())
                .map(r -> UpcomingItem.from(r, today))
                .sorted(Comparator.comparingLong(UpcomingItem::ddays))
                .limit(8)
                .toList();
    }

    @SuppressWarnings("unchecked")
    public TmdbDiscoverResponse discoverHighRated(int page) {
        Map<String, Object> body = restClient.get()
                .uri(b -> b.path(DISCOVER_MOVIE_PATH)
                        .queryParam("include_adult", "false")
                        .queryParam("include_video", "false")
                        .queryParam("language", "ko-KR")
                        .queryParam("page", String.valueOf(page))
                        .queryParam("sort_by", "vote_average.desc")
                        .queryParam("vote_average.gte", "7.5")
                        .queryParam("vote_count.gte", "500")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (body == null) return new TmdbDiscoverResponse(List.of(), 1);
        int totalPages = body.get("total_pages") instanceof Number n ? n.intValue() : 1;
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        List<TmdbDiscoverItem> items = results == null ? List.of()
                : results.stream().map(TmdbDiscoverItem::from).toList();
        return new TmdbDiscoverResponse(items, totalPages);
    }

    public TmdbPersonDetail getPersonDetail(Long tmdbId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> personRaw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PERSON_PATH)
                        .queryParam("language", "ko-KR")
                        .build(tmdbId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        @SuppressWarnings("unchecked")
        Map<String, Object> creditsRaw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PERSON_CREDITS_PATH)
                        .queryParam("language", "ko-KR")
                        .build(tmdbId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return personRaw != null ? TmdbPersonDetail.from(personRaw, creditsRaw) : null;
    }
}
