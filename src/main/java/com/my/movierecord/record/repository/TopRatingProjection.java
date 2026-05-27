package com.my.movierecord.record.repository;

public interface TopRatingProjection {
    Long getTmdbId();
    String getMediaType();
    String getTitle();
    String getPosterUrl();
    Double getAvgRating();
    Long getReviewCount();
}
