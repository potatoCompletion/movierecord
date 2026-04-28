package com.my.movierecord.record.repository;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.config.JpaAuditingConfig;
import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.dto.SortOption;
import com.my.movierecord.record.enums.Emotion;
import com.my.movierecord.record.enums.Immersion;
import com.my.movierecord.record.enums.Story;
import com.my.movierecord.record.enums.Taste;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class WatchRecordRepositoryTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    WatchRecordRepository watchRecordRepository;

    User testUser;

    @BeforeEach
    void setUp() {
        testUser = entityManager.persistAndFlush(User.builder()
                .username("testuser")
                .password("password")
                .name("테스트유저")
                .build());
    }

    @Test
    void save_후_createdAt_updatedAt_자동_설정() {
        WatchRecord saved = entityManager.persistAndFlush(buildWatchRecord("테스트", LocalDate.of(2024, 1, 1), "4.5"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_후_findById_성공() {
        WatchRecord saved = entityManager.persistAndFlush(buildWatchRecord("테스트 영화", LocalDate.of(2024, 1, 1), "4.5"));
        entityManager.clear();

        WatchRecord found = watchRecordRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("테스트 영화");
        assertThat(found.getRating()).isEqualByComparingTo("4.5");
        assertThat(found.getImmersion()).isEqualTo(Immersion.GOOD);
    }

    @Test
    void findAll_LATEST_정렬() {
        entityManager.persist(buildWatchRecord("영화A", LocalDate.of(2024, 1, 1), "3.0"));
        entityManager.persist(buildWatchRecord("영화B", LocalDate.of(2024, 6, 1), "4.0"));
        entityManager.persist(buildWatchRecord("영화C", LocalDate.of(2024, 3, 1), "5.0"));
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10, SortOption.LATEST.getSort());
        List<WatchRecord> result = watchRecordRepository.findAll(pageable).getContent();

        assertThat(result).extracting(WatchRecord::getWatchedDate)
                .containsExactly(
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2024, 3, 1),
                        LocalDate.of(2024, 1, 1)
                );
    }

    @Test
    void findAll_RATING_정렬() {
        entityManager.persist(buildWatchRecord("영화A", LocalDate.of(2024, 1, 1), "3.0"));
        entityManager.persist(buildWatchRecord("영화B", LocalDate.of(2024, 2, 1), "5.0"));
        entityManager.persist(buildWatchRecord("영화C", LocalDate.of(2024, 3, 1), "4.0"));
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10, SortOption.RATING.getSort());
        List<WatchRecord> result = watchRecordRepository.findAll(pageable).getContent();

        assertThat(result).extracting(m -> m.getRating().doubleValue())
                .containsExactly(5.0, 4.0, 3.0);
    }

    @Test
    void findAll_TITLE_정렬() {
        entityManager.persist(buildWatchRecord("나", LocalDate.of(2024, 1, 1), "4.0"));
        entityManager.persist(buildWatchRecord("가", LocalDate.of(2024, 2, 1), "3.0"));
        entityManager.persist(buildWatchRecord("다", LocalDate.of(2024, 3, 1), "5.0"));
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10, SortOption.TITLE.getSort());
        List<WatchRecord> result = watchRecordRepository.findAll(pageable).getContent();

        assertThat(result).extracting(WatchRecord::getTitle)
                .containsExactly("가", "나", "다");
    }

    @Test
    void findAll_페이지_분할() {
        entityManager.persist(buildWatchRecord("영화1", LocalDate.of(2024, 1, 1), "4.0"));
        entityManager.persist(buildWatchRecord("영화2", LocalDate.of(2024, 2, 1), "3.0"));
        entityManager.persist(buildWatchRecord("영화3", LocalDate.of(2024, 3, 1), "5.0"));
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 2, SortOption.LATEST.getSort());
        Page<WatchRecord> result = watchRecordRepository.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void delete_후_findById_empty() {
        WatchRecord saved = entityManager.persistAndFlush(buildWatchRecord("영화", LocalDate.of(2024, 1, 1), "4.0"));
        Long id = saved.getId();

        watchRecordRepository.delete(saved);
        entityManager.flush();
        entityManager.clear();

        assertThat(watchRecordRepository.findById(id)).isEmpty();
    }

    @Test
    void update_변경감지_DB_반영() {
        WatchRecord saved = entityManager.persistAndFlush(buildWatchRecord("원래 제목", LocalDate.of(2024, 1, 1), "4.0"));

        saved.update("수정된 제목", LocalDate.of(2024, 6, 1), "수정 한줄평",
                Immersion.NORMAL, Story.SO_SO, Set.of(Emotion.SAD),
                "수정 좋은점", "수정 아쉬운점", Taste.MISMATCH, new BigDecimal("3.0"), null);
        entityManager.flush();
        entityManager.clear();

        WatchRecord found = watchRecordRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("수정된 제목");
        assertThat(found.getRating()).isEqualByComparingTo("3.0");
        assertThat(found.getImmersion()).isEqualTo(Immersion.NORMAL);
    }

    private WatchRecord buildWatchRecord(String title, LocalDate watchedDate, String rating) {
        return WatchRecord.builder()
                .title(title)
                .watchedDate(watchedDate)
                .immersion(Immersion.GOOD)
                .story(Story.CONVINCING)
                .emotions(Set.of(Emotion.FUNNY))
                .taste(Taste.MATCH)
                .rating(new BigDecimal(rating))
                .user(testUser)
                .build();
    }
}
