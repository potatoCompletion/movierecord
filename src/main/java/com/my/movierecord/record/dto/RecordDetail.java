package com.my.movierecord.record.dto;

import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.enums.Emotion;
import com.my.movierecord.tmdb.image.PosterSize;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

public record RecordDetail(
        Long id,
        String title,
        LocalDate watchedDate,
        String thumbnailUrl,   // TMDB CDN 완성 URL (w342), posterPath 없으면 null
        String oneLiner,
        String immersion,
        String story,
        String emotion,
        String goodPoints,
        String badPoints,
        String taste,
        BigDecimal rating,
        String nickname,
        Long userId
) {
    public static RecordDetail from(WatchRecord wr, TmdbImageUrlProvider images) {
        String thumbnailUrl = wr.getContent() != null
                ? images.poster(wr.getContent().getPosterPath(), PosterSize.W342)
                : null;
        String nickname = wr.getUser() != null ? wr.getUser().getDisplayNickname() : null;
        Long userId = wr.getUser() != null ? wr.getUser().getId() : null;
        return new RecordDetail(
                wr.getId(),
                wr.getTitle(),
                wr.getWatchedDate(),
                thumbnailUrl,
                wr.getOneLiner(),
                wr.getImmersion() != null ? wr.getImmersion().getDisplayName() : null,
                wr.getStory() != null ? wr.getStory().getDisplayName() : null,
                wr.getEmotions() != null && !wr.getEmotions().isEmpty()
                        ? wr.getEmotions().stream()
                                .map(Emotion::getDisplayName)
                                .collect(Collectors.joining(", "))
                        : null,
                wr.getGoodPoints(),
                wr.getBadPoints(),
                wr.getTaste() != null ? wr.getTaste().getDisplayName() : null,
                wr.getRating(),
                nickname,
                userId
        );
    }
}
