package com.my.movierecord.tmdb.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 홈 '곧 개봉해요' 캐시 항목. 운영에서는 Redis에 JSON으로 저장되므로 날짜는 ISO 문자열로 보관한다.
 * D-Day는 여기서 계산하지 않고 렌더 시점에 {@link UpcomingCard}가 계산한다.
 */
public record UpcomingItem(
        Long id,
        String title,
        String originalTitle,
        String posterPath,
        String posterUrl,        // 뷰에서 바로 쓰는 완성 URL (w500), 경로 없으면 null
        String releaseDate,      // 한국 극장 개봉일 ISO(yyyy-MM-dd). 조회 실패 시 discover 최초 개봉일
        boolean reRelease        // 최초 개봉일보다 1년 이상 늦게 한국에서 개봉하면 재개봉
) {
    private static final long RE_RELEASE_MIN_DAYS = 365;

    public static UpcomingItem from(UpcomingCandidate candidate, LocalDate krReleaseDate) {
        LocalDate first = candidate.firstReleaseDate();
        LocalDate release = krReleaseDate != null ? krReleaseDate : first;
        return new UpcomingItem(
                candidate.id(),
                candidate.title(),
                candidate.originalTitle(),
                candidate.posterPath(),
                candidate.posterUrl(),
                release.toString(),
                ChronoUnit.DAYS.between(first, release) >= RE_RELEASE_MIN_DAYS
        );
    }
}
