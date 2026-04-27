package com.my.movierecord.movie.dto;

import com.my.movierecord.movie.domain.Movie;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 영화 상세 조회 응답 DTO.
 * Movie 엔티티의 모든 정보를 클라이언트에 전달하는 형식으로 변환한다.
 * enum 값은 한국어 displayName으로 변환되고,
 * 파일 경로는 HTTP 접근 가능한 /uploads/** 경로로 변환된다.
 */
public record MovieDetail(
        Long id,
        String title,
        LocalDate watchedDate,
        String thumbnailUrl,
        String oneLiner,
        String immersion,
        String story,
        String emotion,
        String goodPoints,
        String badPoints,
        String taste,
        BigDecimal rating,
        String nickname
) {
    public static MovieDetail from(Movie movie) {
        String thumbnailUrl = movie.getThumbnailPath() == null ? null : "/uploads/" + movie.getThumbnailPath();
        String nickname = movie.getUser() != null ? movie.getUser().getDisplayNickname() : null;
        return new MovieDetail(
                movie.getId(),
                movie.getTitle(),
                movie.getWatchedDate(),
                thumbnailUrl,
                movie.getOneLiner(),
                movie.getImmersion().getDisplayName(),
                movie.getStory().getDisplayName(),
                movie.getEmotion().getDisplayName(),
                movie.getGoodPoints(),
                movie.getBadPoints(),
                movie.getTaste().getDisplayName(),
                movie.getRating(),
                nickname
        );
    }
}
