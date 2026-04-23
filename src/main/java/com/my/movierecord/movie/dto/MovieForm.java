package com.my.movierecord.movie.dto;

import com.my.movierecord.movie.domain.Movie;
import com.my.movierecord.movie.enums.Emotion;
import com.my.movierecord.movie.enums.Immersion;
import com.my.movierecord.movie.enums.Story;
import com.my.movierecord.movie.enums.Taste;
import com.my.movierecord.movie.service.MovieSaveCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class MovieForm {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하로 입력해주세요.")
    private String title;

    @NotNull(message = "감상일은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate watchedDate;

    @Size(max = 1000, message = "한줄평은 1000자 이하로 입력해주세요.")
    private String oneLiner;

    @NotNull(message = "몰입감을 선택해주세요.")
    private Immersion immersion;

    @NotNull(message = "스토리를 선택해주세요.")
    private Story story;

    @NotNull(message = "감정을 선택해주세요.")
    private Emotion emotion;

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

    public static MovieForm fromEntity(Movie movie) {
        MovieForm form = new MovieForm();
        form.title = movie.getTitle();
        form.watchedDate = movie.getWatchedDate();
        form.oneLiner = movie.getOneLiner();
        form.immersion = movie.getImmersion();
        form.story = movie.getStory();
        form.emotion = movie.getEmotion();
        form.goodPoints = movie.getGoodPoints();
        form.badPoints = movie.getBadPoints();
        form.taste = movie.getTaste();
        form.rating = movie.getRating();
        return form;
    }

    public MovieSaveCommand toCommand(String thumbnailPath) {
        return new MovieSaveCommand(
                title,
                watchedDate,
                thumbnailPath,
                oneLiner,
                immersion,
                story,
                emotion,
                goodPoints,
                badPoints,
                taste,
                rating
        );
    }
}
