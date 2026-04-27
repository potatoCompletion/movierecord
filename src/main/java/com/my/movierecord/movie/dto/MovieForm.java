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

/**
 * 영화 기록 생성/수정 요청 DTO.
 * 웹 폼에서 사용자 입력을 받아 검증하고, 서비스 계층으로 전달할 MovieSaveCommand로 변환한다.
 * 모든 필드는 Bean Validation 애노테이션으로 유효성 검증이 설정되어 있다.
 */
@Getter
@Setter
public class MovieForm {

    // 영화 제목 (필수, 최대 200자)
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하로 입력해주세요.")
    private String title;

    // 영화를 감상한 날짜 (필수, ISO DATE 형식)
    @NotNull(message = "감상일은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate watchedDate;

    // 한줄평 (선택사항, 최대 1000자)
    @Size(max = 1000, message = "한줄평은 1000자 이하로 입력해주세요.")
    private String oneLiner;

    // 몰입감 (필수)
    @NotNull(message = "몰입감을 선택해주세요.")
    private Immersion immersion;

    // 스토리 평가 (필수)
    @NotNull(message = "스토리를 선택해주세요.")
    private Story story;

    // 감정 반응 (필수)
    @NotNull(message = "감정을 선택해주세요.")
    private Emotion emotion;

    // 좋았던 점 (선택사항, 최대 1000자)
    @Size(max = 1000, message = "좋았던 점은 1000자 이하로 입력해주세요.")
    private String goodPoints;

    // 아쉬웠던 점 (선택사항, 최대 1000자)
    @Size(max = 1000, message = "아쉬웠던 점은 1000자 이하로 입력해주세요.")
    private String badPoints;

    // 개인 취향과의 일치도 (필수)
    @NotNull(message = "내 취향 여부를 선택해주세요.")
    private Taste taste;

    // 별점 (필수, 0.0 ~ 5.0)
    @NotNull(message = "별점을 선택해주세요.")
    @DecimalMin(value = "0.0", message = "별점은 0점 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "별점은 5점 이하여야 합니다.")
    private BigDecimal rating;

    /**
     * Movie 엔티티로부터 MovieForm을 생성한다.
     * 영화 수정 폼(/movies/{id}/edit)에서 기존 데이터를 폼에 채울 때 사용된다.
     */
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

    /**
     * MovieForm을 MovieSaveCommand로 변환한다.
     * 폼 유효성 검증이 완료된 후 서비스 계층에 전달할 커맨드 객체를 생성한다.
     * thumbnailPath는 FileStorageService에서 처리된 파일 경로가 전달된다.
     */
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
