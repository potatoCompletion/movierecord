package com.my.movierecord.kobis.dto;

import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.tmdb.image.PosterSize;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;

public record BoxOfficeItemDto(int rank, String movieNm, String posterUrl, Long audiAcc,
                               Long tmdbId, String mediaType) {

    public static BoxOfficeItemDto of(KobisMovieItem item, Content content, TmdbImageUrlProvider images) {
        Long tmdbId = content != null ? content.getId().getTmdbId() : null;
        String mediaType = content != null ? content.getId().getMediaType() : null;
        return new BoxOfficeItemDto(
                Integer.parseInt(item.rank()),
                item.movieNm(),
                content != null ? images.poster(content.getPosterPath(), PosterSize.W342) : null,
                Long.parseLong(item.audiAcc().replace(",", "")),
                tmdbId,
                mediaType);
    }
}
