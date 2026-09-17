package com.my.movierecord.tmdb.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 홈 '곧 개봉해요' 뷰 DTO. 캐시된 {@link UpcomingItem}을 요청 시각 기준으로 변환한다.
 * ddays는 렌더 시점에 계산하므로 캐시가 하루 묵어도 드리프트가 없고, 개봉일이 지난 항목은 제외한다.
 */
public record UpcomingCard(
        Long id,
        String title,
        String originalTitle,
        String posterPath,
        String posterUrl,
        String releaseDateText,  // "MM.dd"
        long ddays,
        boolean reRelease
) {
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("MM.dd");

    public static List<UpcomingCard> fromAll(List<UpcomingItem> items, LocalDate today) {
        return items.stream()
                .map(item -> from(item, today))
                .filter(card -> card.ddays() >= 0)
                .toList();
    }

    static UpcomingCard from(UpcomingItem item, LocalDate today) {
        LocalDate release = LocalDate.parse(item.releaseDate());
        return new UpcomingCard(
                item.id(),
                item.title(),
                item.originalTitle(),
                item.posterPath(),
                item.posterUrl(),
                release.format(DISPLAY_FMT),
                ChronoUnit.DAYS.between(today, release),
                item.reRelease()
        );
    }
}
