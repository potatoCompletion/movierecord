package com.my.movierecord.record.domain;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.record.enums.Emotion;
import com.my.movierecord.record.enums.Immersion;
import com.my.movierecord.record.enums.Story;
import com.my.movierecord.record.enums.Taste;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "watch_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDate watchedDate;

    @Column(columnDefinition = "TEXT")
    private String oneLiner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Immersion immersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Story story;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "watch_record_emotion", joinColumns = @JoinColumn(name = "watch_record_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "emotion", length = 20)
    private Set<Emotion> emotions = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String goodPoints;

    @Column(columnDefinition = "TEXT")
    private String badPoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Taste taste;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private Content content;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public WatchRecord(String title, LocalDate watchedDate, String oneLiner,
                       Immersion immersion, Story story, Set<Emotion> emotions,
                       String goodPoints, String badPoints, Taste taste,
                       BigDecimal rating, User user, Content content) {
        this.title = title;
        this.watchedDate = watchedDate;
        this.oneLiner = oneLiner;
        this.immersion = immersion;
        this.story = story;
        this.emotions = emotions != null ? new HashSet<>(emotions) : new HashSet<>();
        this.goodPoints = goodPoints;
        this.badPoints = badPoints;
        this.taste = taste;
        this.rating = rating;
        this.user = user;
        this.content = content;
    }

    public void update(String title, LocalDate watchedDate, String oneLiner,
                       Immersion immersion, Story story, Set<Emotion> emotions,
                       String goodPoints, String badPoints, Taste taste,
                       BigDecimal rating, Content content) {
        this.title = title;
        this.watchedDate = watchedDate;
        this.oneLiner = oneLiner;
        this.immersion = immersion;
        this.story = story;
        this.emotions = emotions != null ? new HashSet<>(emotions) : new HashSet<>();
        this.goodPoints = goodPoints;
        this.badPoints = badPoints;
        this.taste = taste;
        this.rating = rating;
        this.content = content;
    }
}
