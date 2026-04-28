package com.my.movierecord.record.dto;

import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.enums.Emotion;
import com.my.movierecord.record.enums.Immersion;
import com.my.movierecord.record.enums.Story;
import com.my.movierecord.record.enums.Taste;
import com.my.movierecord.record.service.WatchRecordSaveCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class RecordForm {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하로 입력해주세요.")
    private String title;

    @NotNull(message = "감상일은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate watchedDate;

    private Long tmdbId;
    private String mediaType;
    private String posterPath;

    @Size(max = 1000, message = "한줄평은 1000자 이하로 입력해주세요.")
    private String oneLiner;

    @NotNull(message = "몰입감을 선택해주세요.")
    private Immersion immersion;

    @NotNull(message = "스토리를 선택해주세요.")
    private Story story;

    @NotEmpty(message = "감정을 1개 이상 선택해주세요.")
    private Set<Emotion> emotions = new HashSet<>();

    @Size(max = 1000, message = "좋았던 점은 1000자 이하로 입력해주세요.")
    private String goodPoints;

    @Size(max = 1000, message = "아쉬웠던 점은 1000자 이하로 입력해주세요.")
    private String badPoints;

    @NotNull(message = "내 취향 여부를 선택해주세요.")
    private Taste taste;

    @NotNull(message = "별점을 선택해주세요.")
    @DecimalMin(value = "0.0", message = "별점은 0점 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "별점은 5점 이하여야 합니다.")
    private BigDecimal rating;

    public static RecordForm fromEntity(WatchRecord wr) {
        RecordForm form = new RecordForm();
        form.title = wr.getTitle();
        form.watchedDate = wr.getWatchedDate();
        form.oneLiner = wr.getOneLiner();
        form.immersion = wr.getImmersion();
        form.story = wr.getStory();
        form.emotions = new HashSet<>(wr.getEmotions());
        form.goodPoints = wr.getGoodPoints();
        form.badPoints = wr.getBadPoints();
        form.taste = wr.getTaste();
        form.rating = wr.getRating();
        if (wr.getContent() != null) {
            form.tmdbId = wr.getContent().getId();
            form.mediaType = wr.getContent().getMediaType();
        }
        return form;
    }

    public WatchRecordSaveCommand toCommand(Long userId) {
        return new WatchRecordSaveCommand(
                title,
                watchedDate,
                tmdbId,
                mediaType,
                posterPath,
                oneLiner,
                immersion,
                story,
                emotions,
                goodPoints,
                badPoints,
                taste,
                rating,
                userId
        );
    }
}
