package com.my.movierecord.kobis.repository;

import com.my.movierecord.kobis.domain.DailyBoxOffice;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyBoxOfficeRepository extends JpaRepository<DailyBoxOffice, Long> {

    @Query("SELECT e FROM DailyBoxOffice e LEFT JOIN FETCH e.content WHERE e.targetDt = :targetDt ORDER BY e.rank ASC")
    List<DailyBoxOffice> findByTargetDtOrderByRankAsc(@Param("targetDt") LocalDate targetDt);
}
