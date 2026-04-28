package com.my.movierecord.record.dto;

import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.enums.Emotion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

public record RecordDetail(
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
        String nickname,
        Long userId
) {
    public static RecordDetail from(WatchRecord wr) {
        String thumbnailUrl = (wr.getContent() != null && wr.getContent().getThumbnailPath() != null)
                ? "/uploads/" + wr.getContent().getThumbnailPath()
                : null;
        String nickname = wr.getUser() != null ? wr.getUser().getDisplayNickname() : null;
        Long userId = wr.getUser() != null ? wr.getUser().getId() : null;
        return new RecordDetail(
                wr.getId(),
                wr.getTitle(),
                wr.getWatchedDate(),
                thumbnailUrl,
                wr.getOneLiner(),
                wr.getImmersion().getDisplayName(),
                wr.getStory().getDisplayName(),
                wr.getEmotions().stream()
                        .map(Emotion::getDisplayName)
                        .collect(Collectors.joining(", ")),
                wr.getGoodPoints(),
                wr.getBadPoints(),
                wr.getTaste().getDisplayName(),
                wr.getRating(),
                nickname,
                userId
        );
    }
}
