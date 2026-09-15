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

    /**
     * TMDB poster_path 원본 값 ("/abc123.jpg" 형태). 렌더링 시 {@code TmdbImageUrlProvider} 가 CDN URL 로 조립한다.
     * 로컬 캐싱 시절에 만들어진 레코드는 null 일 수 있다.
     */
    @Column(length = 255)
    private String posterPath;

    public static Content of(Long tmdbId, String mediaType) {
        Content content = new Content();
        content.id = ContentId.of(tmdbId, mediaType);
        return content;
    }

    public void updatePosterPath(String posterPath) {
        this.posterPath = posterPath;
    }
}
