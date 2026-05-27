package com.my.movierecord.record.repository;

import com.my.movierecord.record.domain.WatchRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WatchRecordRepository extends JpaRepository<WatchRecord, Long> {

    @Query("SELECT wr FROM WatchRecord wr LEFT JOIN FETCH wr.content LEFT JOIN FETCH wr.user LEFT JOIN FETCH wr.emotions WHERE wr.id = :id")
    Optional<WatchRecord> findByIdWithFetch(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "content"})
    @Override
    Page<WatchRecord> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "content"})
    Page<WatchRecord> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT wr FROM WatchRecord wr LEFT JOIN FETCH wr.user WHERE wr.content.id.tmdbId = :tmdbId AND wr.content.id.mediaType = :mediaType ORDER BY wr.createdAt DESC")
    List<WatchRecord> findByContent(@Param("tmdbId") Long tmdbId, @Param("mediaType") String mediaType);

    @Query("""
            SELECT wr.content.id.tmdbId       AS tmdbId,
                   wr.content.id.mediaType    AS mediaType,
                   wr.title                   AS title,
                   wr.content.thumbnailPath   AS posterUrl,
                   AVG(wr.rating)             AS avgRating,
                   COUNT(wr)                  AS reviewCount
            FROM WatchRecord wr
            WHERE wr.rating IS NOT NULL
                       AND wr.createdAt >= :startDateTime
            GROUP BY wr.content.id.tmdbId,
                     wr.content.id.mediaType,
                     wr.title,
                     wr.content.thumbnailPath
            ORDER BY AVG(wr.rating) DESC, COUNT(wr) DESC
            """)
    List<TopRatingProjection> findTopRated(@Param("startDateTime")LocalDateTime startDateTime, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "content"})
    List<WatchRecord> findTop4ByOrderByCreatedAtDesc();
}
