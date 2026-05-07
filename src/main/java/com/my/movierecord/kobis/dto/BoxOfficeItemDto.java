package com.my.movierecord.kobis.dto;

import com.my.movierecord.kobis.domain.DailyBoxOffice;

public record BoxOfficeItemDto(int rank, String movieNm, String posterUrl, Long audiAcc) {

    public static BoxOfficeItemDto fromEntity(DailyBoxOffice e) {
        return new BoxOfficeItemDto(
                e.getRank(),
                e.getMovieNm(),
                e.getContent() != null && e.getContent().getThumbnailPath() != null
                        ? "/uploads/" + e.getContent().getThumbnailPath()
                        : null,
                e.getAudiAcc());
    }
}
