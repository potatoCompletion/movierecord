package com.my.movierecord.movie.dto;

import com.my.movierecord.movie.domain.Movie;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 영화 목록 항목 응답 DTO (record).
 * 영화 목록 페이지에서 각 영화의 주요 정보를 표시할 때 사용된다.
 * 썸네일과 기본 정보(제목, 감상일, 별점)는 여기에 포함되고,
 * 상세 정보는 nested된 MovieDetail에 포함된다.
 */
public record MovieListItem(
        Long id,
        String title,
        LocalDate watchedDate,
        String thumbnailUrl,
        BigDecimal rating,
        MovieDetail detail  // 모든 상세 정보를 포함하는 nested 객체
) {
    /**
     * Movie 엔티티를 MovieListItem DTO로 변환한다.
     * - thumbnailPath: null이면 null, 아니면 /uploads/ 경로로 변환하여 웹 접근 가능하게 함
     * - detail: MovieDetail.from()을 호출하여 enum 필드를 한글 displayName으로 변환한 상세 정보 생성
     */
    public static MovieListItem from(Movie movie) {
        String thumbnailUrl = movie.getThumbnailPath() == null ? null : "/uploads/" + movie.getThumbnailPath();
        return new MovieListItem(
                movie.getId(),
                movie.getTitle(),
                movie.getWatchedDate(),
                thumbnailUrl,
                movie.getRating(),
                MovieDetail.from(movie)
        );
    }
}
