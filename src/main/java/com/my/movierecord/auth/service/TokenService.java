package com.my.movierecord.auth.service;

import com.my.movierecord.auth.domain.RefreshToken;
import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.dto.TokenPair;
import com.my.movierecord.auth.enums.UserStatus;
import com.my.movierecord.auth.exception.ExpiredRefreshTokenException;
import com.my.movierecord.auth.exception.InvalidRefreshTokenException;
import com.my.movierecord.auth.repository.RefreshTokenRepository;
import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.auth.security.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * 액세스/리프레시 토큰 발급·회전·폐기.
 *
 * <p>리프레시 토큰은 원문이 아니라 SHA-256 해시로 DB에 저장(원장)하고, Redis에는 조회 캐시로
 * write-through 한다(best-effort). 회전 시 이전 토큰을 폐기(rotation)하고, 이미 폐기된 토큰이
 * 다시 제출되면 같은 family 전체를 폐기한다(재사용 탐지). 매 회전마다 사용자 상태를 DB에서 재확인한다.
 */
@Service
@Slf4j
public class TokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCacheService cacheService;
    private final JwtProvider jwtProvider;
    private final Duration refreshTtl;

    public TokenService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        RefreshTokenCacheService cacheService,
                        JwtProvider jwtProvider,
                        @Value("${app.jwt.refresh-token-ttl-seconds:1209600}") long refreshTtlSeconds) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cacheService = cacheService;
        this.jwtProvider = jwtProvider;
        this.refreshTtl = Duration.ofSeconds(refreshTtlSeconds);
    }

    @Transactional
    public TokenPair issueTokenPair(User user) {
        return issue(user, UUID.randomUUID().toString());
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);

        // Redis 우선 조회(서킷브레이커 보호). 장애/서킷 오픈 시 예외를 삼키고 DB 원장으로 폴백한다.
        boolean cacheHit = lookupCache(hash).isPresent();
        log.debug("리프레시 캐시 {} (hash 앞 8자리={})", cacheHit ? "HIT" : "MISS/폴백", hash.substring(0, 8));

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));

        LocalDateTime now = LocalDateTime.now();

        if (token.isRevoked()) {
            // 이미 폐기된 토큰의 재제출 → 재사용 탐지: family 전체 폐기.
            revokeFamily(token.getFamilyId(), now);
            throw new InvalidRefreshTokenException("Refresh token reuse detected");
        }
        if (token.isExpired(now)) {
            token.revoke(now);
            evictCache(hash);
            throw new ExpiredRefreshTokenException("Refresh token expired");
        }

        // 원자적 one-time-use 소비(CAS): WHERE revoked_at IS NULL 조건부 UPDATE 로 동시 회전을 직렬화한다.
        // 동일 토큰으로 두 요청이 동시에 들어와도 DB 행 잠금으로 정확히 하나만 1행을 갱신(승자)하고,
        // 나머지는 0행 → 이미 회전/폐기된 것으로 간주해 재사용 탐지(family 전체 폐기)를 트리거한다.
        // (findByTokenHash 스냅샷만으로는 TOCTOU 레이스를 막을 수 없어 이 원자적 갱신이 유일한 게이트다.)
        int claimed = refreshTokenRepository.revokeIfActive(hash, now);
        if (claimed == 0) {
            revokeFamily(token.getFamilyId(), now);
            throw new InvalidRefreshTokenException("Refresh token reuse detected");
        }
        evictCache(hash);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found for refresh token"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            revokeFamily(token.getFamilyId(), now);
            throw new InvalidRefreshTokenException("User is not active: " + user.getStatus());
        }

        // 회전: 같은 family 로 새 쌍 발급(현재 토큰은 위 CAS 로 이미 폐기됨).
        return issue(user, token.getFamilyId());
    }

    /** 회원 탈퇴/강제 로그아웃 시 해당 사용자의 모든 리프레시 토큰을 폐기한다(재갱신 차단). */
    @Transactional
    public void revokeAllForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId).forEach(token -> {
            token.revoke(now);
            evictCache(token.getTokenHash());
        });
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.revoke(LocalDateTime.now());
            evictCache(hash);
        });
    }

    private TokenPair issue(User user, String familyId) {
        String accessToken = jwtProvider.createAccessToken(
                user.getUsername(), user.getId(), user.getRole(), user.getDisplayNickname());

        String rawRefreshToken = generateRawRefreshToken();
        String hash = sha256(rawRefreshToken);
        LocalDateTime now = LocalDateTime.now();

        RefreshToken entity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash)
                .familyId(familyId)
                .issuedAt(now)
                .expiresAt(now.plus(refreshTtl))
                .build();
        refreshTokenRepository.save(entity);
        putCache(hash, user.getId());

        return new TokenPair(accessToken, rawRefreshToken);
    }

    private void revokeFamily(String familyId, LocalDateTime now) {
        refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull(familyId).forEach(t -> {
            t.revoke(now);
            evictCache(t.getTokenHash());
        });
    }

    // ─── Redis 캐시 접근은 best-effort: 장애/서킷 오픈 시 예외를 삼키고 DB 원장을 신뢰한다. ───

    private Optional<Long> lookupCache(String hash) {
        try {
            return cacheService.findUserId(hash);
        } catch (Exception e) {
            log.warn("Redis 리프레시 캐시 조회 실패 → DB 폴백: {}", e.toString());
            return Optional.empty();
        }
    }

    private void putCache(String hash, Long userId) {
        try {
            cacheService.put(hash, userId, refreshTtl);
        } catch (Exception e) {
            log.warn("Redis 리프레시 캐시 쓰기 실패(무시, DB 원장 유지): {}", e.toString());
        }
    }

    private void evictCache(String hash) {
        try {
            cacheService.evict(hash);
        } catch (Exception e) {
            log.warn("Redis 리프레시 캐시 삭제 실패(무시): {}", e.toString());
        }
    }

    private static String generateRawRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
