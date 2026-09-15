package com.my.movierecord.record.dto;

import com.my.movierecord.record.repository.TopRatingProjection;
import com.my.movierecord.tmdb.image.PosterSize;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;

/** 홈 "이 주의 명화" 섹션용. 프로젝션의 posterPath 를 TMDB CDN URL 로 완성해 뷰에 넘긴다. */
public record TopRatingItem(
        Long tmdbId,
        String mediaType,
        String title,
        String posterUrl,     // 완성 URL (w342), 경로 없으면 null
        Double avgRating,
        Long reviewCount
) {
    public static TopRatingItem from(TopRatingProjection p, TmdbImageUrlProvider images) {
        return new TopRatingItem(
                p.getTmdbId(),
                p.getMediaType(),
                p.getTitle(),
                images.poster(p.getPosterPath(), PosterSize.W342),
                p.getAvgRating(),
                p.getReviewCount()
        );
    }
}
