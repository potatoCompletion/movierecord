package com.my.movierecord.spotlight.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "spotlight_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SpotlightHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tmdbId;

    @Column(length = 20)
    private String imdbId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String originalTitle;

    @Column(length = 500)
    private String posterPath;

    @Column(length = 500)
    private String backdropPath;

    @Column(length = 4)
    private String releaseYear;

    @Column(columnDefinition = "TEXT")
    private String overview;

    private Double tmdbRating;

    @Column(length = 10)
    private String rtScore;

    @Column(nullable = false)
    private LocalDate selectedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public SpotlightHistory(Long tmdbId, String imdbId, String title, String originalTitle,
            String posterPath, String backdropPath, String releaseYear,
            String overview, Double tmdbRating, String rtScore, LocalDate selectedAt) {
        this.tmdbId = tmdbId;
        this.imdbId = imdbId;
        this.title = title;
        this.originalTitle = originalTitle;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.releaseYear = releaseYear;
        this.overview = overview;
        this.tmdbRating = tmdbRating;
        this.rtScore = rtScore;
        this.selectedAt = selectedAt;
    }
}
