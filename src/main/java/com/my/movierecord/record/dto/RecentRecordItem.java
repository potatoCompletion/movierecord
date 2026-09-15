package com.my.movierecord.record.dto;

import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.tmdb.image.PosterSize;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 홈 "최근 감상평" 섹션용. 엔티티 대신 뷰가 쓰는 필드만 담고 포스터는 TMDB CDN URL 로 완성한다. */
public record RecentRecordItem(
        Long id,
        String title,
        String posterUrl,     // 완성 URL (w342), 경로 없으면 null
        String nickname,
        LocalDateTime createdAt,
        BigDecimal rating,
        String oneLiner,
        String goodPoints
) {
    public static RecentRecordItem from(WatchRecord wr, TmdbImageUrlProvider images) {
        String posterUrl = wr.getContent() != null
                ? images.poster(wr.getContent().getPosterPath(), PosterSize.W342)
                : null;
        String nickname = wr.getUser() != null ? wr.getUser().getDisplayNickname() : null;
        return new RecentRecordItem(
                wr.getId(),
                wr.getTitle(),
                posterUrl,
                nickname,
                wr.getCreatedAt(),
                wr.getRating(),
                wr.getOneLiner(),
                wr.getGoodPoints()
        );
    }
}
