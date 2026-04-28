package com.my.movierecord.record.dto;

import com.my.movierecord.record.domain.WatchRecord;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordListItem(
        Long id,
        String title,
        LocalDate watchedDate,
        String thumbnailUrl,
        BigDecimal rating,
        RecordDetail detail
) {
    public static RecordListItem from(WatchRecord wr) {
        String thumbnailUrl = (wr.getContent() != null && wr.getContent().getThumbnailPath() != null)
                ? "/uploads/" + wr.getContent().getThumbnailPath()
                : null;
        return new RecordListItem(
                wr.getId(),
                wr.getTitle(),
                wr.getWatchedDate(),
                thumbnailUrl,
                wr.getRating(),
                RecordDetail.from(wr)
        );
    }
}
