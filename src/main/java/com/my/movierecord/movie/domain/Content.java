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

    /**
     * TMDB poster_path 원본 값 ("/abc123.jpg" 형태).
     * 로컬 캐싱(thumbnailPath) 제거를 위한 준비 필드로, 기존 레코드는 null일 수 있다.
     */
    @Column(length = 255)
    private String posterPath;

    public static Content of(Long tmdbId, String mediaType) {
        Content content = new Content();
        content.id = ContentId.of(tmdbId, mediaType);
        return content;
    }

    public void updateThumbnailPath(String path) {
        this.thumbnailPath = path;
    }

    public void updatePosterPath(String posterPath) {
        this.posterPath = posterPath;
    }
}
