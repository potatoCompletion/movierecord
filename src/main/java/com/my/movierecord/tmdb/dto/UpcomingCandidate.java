package com.my.movierecord.tmdb.dto;

import com.my.movierecord.tmdb.image.PosterSize;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;
import java.time.LocalDate;
import java.util.Map;

/**
 * TMDB discover 응답 한 건. 한국 개봉일이 아직 확정되지 않은 중간 상태로, 캐시에 저장되지 않는다.
 * {@code firstReleaseDate}는 discover가 돌려주는 최초(전세계) 개봉일이다.
 */
public record UpcomingCandidate(
        Long id,
        String title,
        String originalTitle,
        String posterPath,
        String posterUrl,
        LocalDate firstReleaseDate
) {
    public static UpcomingCandidate from(Map<String, Object> raw, TmdbImageUrlProvider images) {
        Long id = raw.get("id") instanceof Number n ? n.longValue() : null;
        String posterPath = (String) raw.get("poster_path");
        return new UpcomingCandidate(
                id,
                (String) raw.get("title"),
                (String) raw.get("original_title"),
                posterPath,
                images.poster(posterPath, PosterSize.W500),
                LocalDate.parse((String) raw.get("release_date"))
        );
    }
}
