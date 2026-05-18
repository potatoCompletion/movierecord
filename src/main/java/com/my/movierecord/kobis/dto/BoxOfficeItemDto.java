package com.my.movierecord.kobis.dto;

import com.my.movierecord.kobis.domain.DailyBoxOffice;

public record BoxOfficeItemDto(int rank, String movieNm, String posterUrl, Long audiAcc,
                               Long tmdbId, String mediaType) {

    public static BoxOfficeItemDto fromEntity(DailyBoxOffice e) {
        Long tmdbId = e.getContent() != null ? e.getContent().getId().getTmdbId() : null;
        String mediaType = e.getContent() != null ? e.getContent().getId().getMediaType() : null;
        return new BoxOfficeItemDto(
                e.getRank(),
                e.getMovieNm(),
                e.getContent() != null && e.getContent().getThumbnailPath() != null
                        ? "/uploads/" + e.getContent().getThumbnailPath()
                        : null,
                e.getAudiAcc(),
                tmdbId,
                mediaType);
    }
}
