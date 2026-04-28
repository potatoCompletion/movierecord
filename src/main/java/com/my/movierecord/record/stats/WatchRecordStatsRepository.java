package com.my.movierecord.record.stats;

import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.enums.Taste;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WatchRecordStatsRepository extends JpaRepository<WatchRecord, Long> {

    long countByUserId(Long userId);

    @Query("SELECT COUNT(w) FROM WatchRecord w WHERE w.user.id = :userId AND YEAR(w.watchedDate) = :year")
    long countByUserIdAndYear(@Param("userId") Long userId, @Param("year") int year);

    @Query("SELECT COUNT(w) FROM WatchRecord w WHERE w.user.id = :userId AND YEAR(w.watchedDate) = :year AND MONTH(w.watchedDate) = :month")
    long countByUserIdAndYearAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT AVG(w.rating) FROM WatchRecord w WHERE w.user.id = :userId")
    BigDecimal averageRatingByUserId(@Param("userId") Long userId);

    long countByUserIdAndTaste(Long userId, Taste taste);

    @Query("SELECT YEAR(w.watchedDate), MONTH(w.watchedDate), COUNT(w) FROM WatchRecord w WHERE w.user.id = :userId AND w.watchedDate >= :from AND w.watchedDate <= :to GROUP BY YEAR(w.watchedDate), MONTH(w.watchedDate)")
    List<Object[]> countMonthlyByUserIdAndDateRange(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT e, COUNT(w) FROM WatchRecord w JOIN w.emotions e WHERE w.user.id = :userId GROUP BY e")
    List<Object[]> countByUserIdGroupByEmotion(@Param("userId") Long userId);
}
