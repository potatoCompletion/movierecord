package com.my.movierecord.auth.repository;

import com.my.movierecord.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 사용자 엔티티에 대한 데이터 접근 객체.
 * Spring Data JPA를 통해 기본 CRUD 연산과 커스텀 쿼리를 제공한다.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * 사용자명으로 사용자를 조회한다.
     */
    Optional<User> findByUsername(String username);

    /**
     * 주어진 사용자명이 이미 존재하는지 확인한다.
     */
    boolean existsByUsername(String username);
}
