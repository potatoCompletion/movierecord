package com.my.movierecord.record.dto;

import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.enums.Emotion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RecordListItem(
        Long id,
        String title,
        LocalDate watchedDate,
        String thumbnailUrl,
        BigDecimal rating,
        RecordDetail detail,
        List<Emotion> emotions
) {
    public static RecordListItem from(WatchRecord wr) {
        String thumbnailUrl = (wr.getContent() != null && wr.getContent().getThumbnailPath() != null)
                ? "/uploads/" + wr.getContent().getThumbnailPath()
                : null;
        List<Emotion> emotions = (wr.getEmotions() != null)
                ? wr.getEmotions().stream().toList()
                : List.of();
        return new RecordListItem(
                wr.getId(),
                wr.getTitle(),
                wr.getWatchedDate(),
                thumbnailUrl,
                wr.getRating(),
                RecordDetail.from(wr),
                emotions
        );
    }
}
