package com.my.movierecord.auth.repository;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByUsername(String username);
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndIdNot(String nickname, Long id);
    List<User> findAllByStatus(UserStatus status);
    List<User> findAllByStatusNotOrderByCreatedAtDesc(UserStatus status);
    @Query("""
           SELECT u FROM User u WHERE u.status <> :status
           ORDER BY CASE WHEN u.role = 'ROLE_ADMIN' THEN 0
                         WHEN u.status = 'PENDING' THEN 1 ELSE 2 END,
           u.createdAt DESC
           """)
    List<User> findAllForAdmin(@Param("status") UserStatus status);
    List<User> findAllByOrderByCreatedAtDesc();
}
