package com.my.movierecord.movie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentId implements Serializable {

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType;

    public static ContentId of(Long tmdbId, String mediaType) {
        ContentId id = new ContentId();
        id.tmdbId = tmdbId;
        id.mediaType = mediaType;
        return id;
    }
}
