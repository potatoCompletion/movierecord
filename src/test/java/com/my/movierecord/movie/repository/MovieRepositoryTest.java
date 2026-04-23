package com.my.movierecord.movie.repository;

import com.my.movierecord.config.JpaAuditingConfig;
import com.my.movierecord.movie.domain.Movie;
import com.my.movierecord.movie.dto.SortOption;
import com.my.movierecord.movie.enums.Emotion;
import com.my.movierecord.movie.enums.Immersion;
import com.my.movierecord.movie.enums.Story;
import com.my.movierecord.movie.enums.Taste;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
class MovieRepositoryTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    MovieRepository movieRepository;

    @Test
    void save_후_createdAt_updatedAt_자동_설정() {
        Movie saved = entityManager.persistAndFlush(buildMovie("테스트", LocalDate.of(2024, 1, 1), "4.5"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_후_findById_성공() {
        Movie saved = entityManager.persistAndFlush(buildMovie("테스트 영화", LocalDate.of(2024, 1, 1), "4.5"));
        entityManager.clear();

        Movie found = movieRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("테스트 영화");
        assertThat(found.getRating()).isEqualByComparingTo("4.5");
        assertThat(found.getImmersion()).isEqualTo(Immersion.GOOD);
    }

    @Test
    void findAll_LATEST_정렬() {
        entityManager.persist(buildMovie("영화A", LocalDate.of(2024, 1, 1), "3.0"));
        entityManager.persist(buildMovie("영화B", LocalDate.of(2024, 6, 1), "4.0"));
        entityManager.persist(buildMovie("영화C", LocalDate.of(2024, 3, 1), "5.0"));
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10, SortOption.LATEST.getSort());
        List<Movie> result = movieRepository.findAll(pageable).getContent();

        assertThat(result).extracting(Movie::getWatchedDate)
                .containsExactly(
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2024, 3, 1),
                        LocalDate.of(2024, 1, 1)
                );
    }

    @Test
    void findAll_RATING_정렬() {
        entityManager.persist(buildMovie("영화A", LocalDate.of(2024, 1, 1), "3.0"));
        entityManager.persist(buildMovie("영화B", LocalDate.of(2024, 2, 1), "5.0"));
        entityManager.persist(buildMovie("영화C", LocalDate.of(2024, 3, 1), "4.0"));
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10, SortOption.RATING.getSort());
        List<Movie> result = movieRepository.findAll(pageable).getContent();

        assertThat(result).extracting(m -> m.getRating().doubleValue())
                .containsExactly(5.0, 4.0, 3.0);
    }

    @Test
    void findAll_TITLE_정렬() {
        entityManager.persist(buildMovie("나", LocalDate.of(2024, 1, 1), "4.0"));
        entityManager.persist(buildMovie("가", LocalDate.of(2024, 2, 1), "3.0"));
        entityManager.persist(buildMovie("다", LocalDate.of(2024, 3, 1), "5.0"));
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10, SortOption.TITLE.getSort());
        List<Movie> result = movieRepository.findAll(pageable).getContent();

        assertThat(result).extracting(Movie::getTitle)
                .containsExactly("가", "나", "다");
    }

    @Test
    void findAll_페이지_분할() {
        entityManager.persist(buildMovie("영화1", LocalDate.of(2024, 1, 1), "4.0"));
        entityManager.persist(buildMovie("영화2", LocalDate.of(2024, 2, 1), "3.0"));
        entityManager.persist(buildMovie("영화3", LocalDate.of(2024, 3, 1), "5.0"));
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 2, SortOption.LATEST.getSort());
        Page<Movie> result = movieRepository.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void delete_후_findById_empty() {
        Movie saved = entityManager.persistAndFlush(buildMovie("영화", LocalDate.of(2024, 1, 1), "4.0"));
        Long id = saved.getId();

        movieRepository.delete(saved);
        entityManager.flush();
        entityManager.clear();

        assertThat(movieRepository.findById(id)).isEmpty();
    }

    @Test
    void update_변경감지_DB_반영() {
        Movie saved = entityManager.persistAndFlush(buildMovie("원래 제목", LocalDate.of(2024, 1, 1), "4.0"));

        saved.update("수정된 제목", LocalDate.of(2024, 6, 1), null, "수정 한줄평",
                Immersion.NORMAL, Story.SO_SO, Emotion.SAD,
                "수정 좋은점", "수정 아쉬운점", Taste.MISMATCH, new BigDecimal("3.0"));
        entityManager.flush();
        entityManager.clear();

        Movie found = movieRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("수정된 제목");
        assertThat(found.getRating()).isEqualByComparingTo("3.0");
        assertThat(found.getImmersion()).isEqualTo(Immersion.NORMAL);
    }

    private Movie buildMovie(String title, LocalDate watchedDate, String rating) {
        return Movie.builder()
                .title(title)
                .watchedDate(watchedDate)
                .immersion(Immersion.GOOD)
                .story(Story.CONVINCING)
                .emotion(Emotion.FUNNY)
                .taste(Taste.MATCH)
                .rating(new BigDecimal(rating))
                .build();
    }
}
