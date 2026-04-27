package com.my.movierecord.movie.service;

import com.my.movierecord.movie.enums.Emotion;
import com.my.movierecord.movie.enums.Immersion;
import com.my.movierecord.movie.enums.Story;
import com.my.movierecord.movie.enums.Taste;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 영화 저장 커맨드 record.
 * 컨트롤러에서 검증된 MovieForm을 서비스 계층으로 전달할 때 사용되는 DTO이다.
 * 파일 업로드 처리된 thumbnailPath를 포함하여 service 계층에 전달한다.
 *
 * 흐름: MovieForm → MovieSaveCommand(toCommand에서 변환) → MovieService → Movie entity로 변환
 */
public record MovieSaveCommand(
        String title,
        LocalDate watchedDate,
        String thumbnailPath,  // FileStorageService에서 처리된 UUID 파일명
        String oneLiner,
        Immersion immersion,
        Story story,
        Emotion emotion,
        String goodPoints,
        String badPoints,
        Taste taste,
        BigDecimal rating,
        Long userId
) {}
