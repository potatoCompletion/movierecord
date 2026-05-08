package com.my.movierecord.movie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "content")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content {

    @EmbeddedId
    private ContentId id;

    @Column(length = 500)
    private String thumbnailPath;

    public static Content of(Long tmdbId, String mediaType) {
        Content content = new Content();
        content.id = ContentId.of(tmdbId, mediaType);
        return content;
    }

    public void updateThumbnailPath(String path) {
        this.thumbnailPath = path;
    }
}
