package com.my.movierecord.spotlight.repository;

import com.my.movierecord.spotlight.domain.SpotlightHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotlightHistoryRepository extends JpaRepository<SpotlightHistory, Long> {

    Optional<SpotlightHistory> findTopByOrderBySelectedAtDesc();
}
