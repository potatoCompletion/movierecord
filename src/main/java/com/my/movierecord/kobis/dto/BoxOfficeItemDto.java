package com.my.movierecord.kobis.dto;

import com.my.movierecord.movie.domain.Content;

public record BoxOfficeItemDto(int rank, String movieNm, String posterUrl, Long audiAcc,
                               Long tmdbId, String mediaType) {

    public static BoxOfficeItemDto of(KobisMovieItem item, Content content) {
        Long tmdbId = content != null ? content.getId().getTmdbId() : null;
        String mediaType = content != null ? content.getId().getMediaType() : null;
        return new BoxOfficeItemDto(
                Integer.parseInt(item.rank()),
                item.movieNm(),
                content != null && content.getThumbnailPath() != null
                        ? "/uploads/" + content.getThumbnailPath()
                        : null,
                Long.parseLong(item.audiAcc().replace(",", "")),
                tmdbId,
                mediaType);
    }
}
