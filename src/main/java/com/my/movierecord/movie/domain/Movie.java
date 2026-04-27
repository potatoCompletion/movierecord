package com.my.movierecord.movie.domain;

import com.my.movierecord.movie.enums.Emotion;
import com.my.movierecord.movie.enums.Immersion;
import com.my.movierecord.movie.enums.Story;
import com.my.movierecord.movie.enums.Taste;
import com.my.movierecord.auth.domain.User;
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

/**
 * 영화 기록 엔티티.
 * 사용자가 감상한 영화의 정보를 저장하고 관리한다.
 * JPA Auditing을 통해 생성일시와 수정일시를 자동으로 관리한다.
 */
@Entity
@Table(name = "movie")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 영화 제목 (필수, 최대 200자)
    @Column(nullable = false, length = 200)
    private String title;

    // 영화를 감상한 날짜 (필수)
    @Column(nullable = false)
    private LocalDate watchedDate;

    // 썸네일 이미지 파일 경로 (UUID 파일명으로 저장됨, 선택사항)
    @Column(length = 500)
    private String thumbnailPath;

    // 한줄평 (선택사항)
    @Column(columnDefinition = "TEXT")
    private String oneLiner;

    // 몰입감 정도 (필수, Immersion enum: GOOD, NORMAL, BAD)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Immersion immersion;

    // 스토리 평가 (필수, Story enum: CONVINCING, SO_SO, BAD)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Story story;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "movie_emotion", joinColumns = @JoinColumn(name = "movie_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "emotion", length = 20)
    private Set<Emotion> emotions = new HashSet<>();

    // 좋았던 점 상세 내용 (선택사항)
    @Column(columnDefinition = "TEXT")
    private String goodPoints;

    // 아쉬웠던 점 상세 내용 (선택사항)
    @Column(columnDefinition = "TEXT")
    private String badPoints;

    // 개인 취향과의 일치도 (필수, Taste enum: MATCH, MISMATCH)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Taste taste;

    // 별점 (필수, 0.0 ~ 5.0, 소수점 첫째 자리까지)
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 생성 일시 (JPA Auditing에 의해 자동 설정, 수정 불가)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 마지막 수정 일시 (JPA Auditing에 의해 자동 갱신)
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 빌더 패턴을 이용한 생성자.
     * MovieService에서 MovieSaveCommand로부터 영화 엔티티를 생성할 때 사용된다.
     */
    @Builder
    public Movie(String title, LocalDate watchedDate, String thumbnailPath,
                 String oneLiner, Immersion immersion, Story story, Set<Emotion> emotions,
                 String goodPoints, String badPoints, Taste taste, BigDecimal rating,
                 User user) {
        this.title = title;
        this.watchedDate = watchedDate;
        this.thumbnailPath = thumbnailPath;
        this.oneLiner = oneLiner;
        this.immersion = immersion;
        this.story = story;
        this.emotions = emotions != null ? new HashSet<>(emotions) : new HashSet<>();
        this.goodPoints = goodPoints;
        this.badPoints = badPoints;
        this.taste = taste;
        this.rating = rating;
        this.user = user;
    }

    /**
     * 기존 영화 기록을 업데이트한다.
     * 모든 필드를 새로운 값으로 변경한다.
     * updatedAt 필드는 JPA Auditing에 의해 자동으로 갱신된다.
     */
    public void update(String title, LocalDate watchedDate, String thumbnailPath,
                       String oneLiner, Immersion immersion, Story story, Set<Emotion> emotions,
                       String goodPoints, String badPoints, Taste taste, BigDecimal rating) {
        this.title = title;
        this.watchedDate = watchedDate;
        this.thumbnailPath = thumbnailPath;
        this.oneLiner = oneLiner;
        this.immersion = immersion;
        this.story = story;
        this.emotions = emotions != null ? new HashSet<>(emotions) : new HashSet<>();
        this.goodPoints = goodPoints;
        this.badPoints = badPoints;
        this.taste = taste;
        this.rating = rating;
    }
}
