package com.my.movierecord.record.repository;

import com.my.movierecord.record.domain.WatchRecord;
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
}
