package com.my.movierecord.kobis.domain;

import com.my.movierecord.movie.domain.Content;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "daily_box_office",
    uniqueConstraints = @UniqueConstraint(columnNames = {"target_dt", "rank"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyBoxOffice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_dt", nullable = false)
    private LocalDate targetDt;

    @Column(nullable = false)
    private int rank;

    @Column(name = "movie_nm", nullable = false, length = 200)
    private String movieNm;

    @Column(name = "audi_acc")
    private Long audiAcc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private Content content;

    public static DailyBoxOffice of(LocalDate targetDt, int rank, String movieNm, Long audiAcc, Content content) {
        DailyBoxOffice entity = new DailyBoxOffice();
        entity.targetDt = targetDt;
        entity.rank = rank;
        entity.movieNm = movieNm;
        entity.audiAcc = audiAcc;
        entity.content = content;
        return entity;
    }
}
