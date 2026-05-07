package com.my.movierecord.movie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "content")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content {

    @Id
    private Long id;

    @Column(nullable = false, length = 20)
    private String mediaType;

    @Column(length = 500)
    private String thumbnailPath;

    public static Content of(Long id, String mediaType) {
        Content content = new Content();
        content.id = id;
        content.mediaType = mediaType;
        return content;
    }

    public void updateThumbnailPath(String path) {
        this.thumbnailPath = path;
    }
}
