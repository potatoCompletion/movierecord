package com.my.movierecord.record.dto;

import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.enums.Emotion;
import com.my.movierecord.tmdb.image.PosterSize;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RecordListItem(
        Long id,
        String title,
        LocalDate watchedDate,
        String thumbnailUrl,   // TMDB CDN 완성 URL (w342), posterPath 없으면 null
        BigDecimal rating,
        RecordDetail detail,
        List<Emotion> emotions
) {
    public static RecordListItem from(WatchRecord wr, TmdbImageUrlProvider images) {
        String thumbnailUrl = wr.getContent() != null
                ? images.poster(wr.getContent().getPosterPath(), PosterSize.W342)
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
                RecordDetail.from(wr, images),
                emotions
        );
    }
}
