package com.my.movierecord.movie.repository;

import com.my.movierecord.movie.domain.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    @EntityGraph(attributePaths = {"user", "emotions"})
    @Override
    Page<Movie> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "emotions"})
    Page<Movie> findByUserId(Long userId, Pageable pageable);
}
