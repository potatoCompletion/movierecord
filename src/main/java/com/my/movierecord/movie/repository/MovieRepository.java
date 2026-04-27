package com.my.movierecord.movie.repository;

import com.my.movierecord.movie.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 영화 기록 데이터 접근 계층 인터페이스.
 * JpaRepository를 확장하여 Spring Data JPA의 기본 CRUD 및 페이징 기능을 제공한다.
 *
 * 상속된 메서드:
 * - findAll(Pageable): 페이지 단위로 전체 영화 조회
 * - findById(Long): ID로 단일 영화 조회
 * - save(Movie): 영화 기록 저장 (생성 또는 수정)
 * - delete(Movie): 영화 기록 삭제
 *
 * MovieService에서 주입받아 비즈니스 로직을 처리한다.
 */
public interface MovieRepository extends JpaRepository<Movie, Long> {
}
