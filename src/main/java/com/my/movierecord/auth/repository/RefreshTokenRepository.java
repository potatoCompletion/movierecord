package com.my.movierecord.auth.repository;

import com.my.movierecord.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** 재사용 탐지 시 같은 회전 체인의 미폐기 토큰 전체를 폐기하기 위해 사용. */
    List<RefreshToken> findAllByFamilyIdAndRevokedAtIsNull(String familyId);

    /** 회원 탈퇴 등으로 특정 사용자의 모든 리프레시 토큰을 폐기하기 위해 사용. */
    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);

    /**
     * 원자적 one-time-use 소비: 아직 미폐기(revoked_at IS NULL)인 경우에만 폐기하고 갱신된 행 수를 반환한다.
     * 동시 회전 요청을 DB 행 잠금으로 직렬화해 정확히 하나만 1을 받게 하여 TOCTOU 레이스를 차단한다.
     */
    @Modifying
    @Query("update RefreshToken r set r.revokedAt = :now where r.tokenHash = :hash and r.revokedAt is null")
    int revokeIfActive(@Param("hash") String hash, @Param("now") LocalDateTime now);
}
