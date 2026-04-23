package com.my.movierecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "movie")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDate watchedDate;

    @Column(length = 500)
    private String thumbnailPath;

    @Column(columnDefinition = "TEXT")
    private String oneLiner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Immersion immersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Story story;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Emotion emotion;

    @Column(columnDefinition = "TEXT")
    private String goodPoints;

    @Column(columnDefinition = "TEXT")
    private String badPoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Taste taste;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Movie(String title, LocalDate watchedDate, String thumbnailPath,
                 String oneLiner, Immersion immersion, Story story, Emotion emotion,
                 String goodPoints, String badPoints, Taste taste, BigDecimal rating) {
        this.title = title;
        this.watchedDate = watchedDate;
        this.thumbnailPath = thumbnailPath;
        this.oneLiner = oneLiner;
        this.immersion = immersion;
        this.story = story;
        this.emotion = emotion;
        this.goodPoints = goodPoints;
        this.badPoints = badPoints;
        this.taste = taste;
        this.rating = rating;
    }

    public void update(String title, LocalDate watchedDate, String thumbnailPath,
                       String oneLiner, Immersion immersion, Story story, Emotion emotion,
                       String goodPoints, String badPoints, Taste taste, BigDecimal rating) {
        this.title = title;
        this.watchedDate = watchedDate;
        this.thumbnailPath = thumbnailPath;
        this.oneLiner = oneLiner;
        this.immersion = immersion;
        this.story = story;
        this.emotion = emotion;
        this.goodPoints = goodPoints;
        this.badPoints = badPoints;
        this.taste = taste;
        this.rating = rating;
    }
}
