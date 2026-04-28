package com.my.movierecord.record.repository;

import com.my.movierecord.record.domain.WatchRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchRecordRepository extends JpaRepository<WatchRecord, Long> {

    @EntityGraph(attributePaths = {"user", "content", "emotions"})
    @Override
    Page<WatchRecord> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "content", "emotions"})
    Page<WatchRecord> findByUserId(Long userId, Pageable pageable);
}
