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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private RefreshTokenCacheService cacheService;
    @Mock
    private JwtProvider jwtProvider;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(userRepository, refreshTokenRepository, cacheService, jwtProvider, 1209600);
    }

    @Test
    @DisplayName("issueTokenPair: 액세스 토큰 발급 + 리프레시 해시 DB 저장 + 캐시 write-through")
    void issueTokenPair_persistsAndCaches() {
        User user = activeUser(1L, "alice");
        given(jwtProvider.createAccessToken(anyString(), any(), anyString(), any())).willReturn("access-jwt");

        TokenPair pair = tokenService.issueTokenPair(user);

        assertThat(pair.accessToken()).isEqualTo("access-jwt");
        assertThat(pair.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(cacheService).put(anyString(), any(), any());
    }

    @Test
    @DisplayName("refresh: 유효 토큰이면 이전 토큰 폐기 후 같은 family 로 회전 발급")
    void refresh_rotatesWithinSameFamily() {
        User user = activeUser(1L, "alice");
        RefreshToken current = validToken(1L, "fam-1");
        given(cacheService.findUserId(anyString())).willReturn(Optional.of(1L));
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(current));
        given(refreshTokenRepository.revokeIfActive(anyString(), any())).willReturn(1);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(anyString(), any(), anyString(), any())).willReturn("new-access");

        TokenPair pair = tokenService.refresh("raw-refresh");

        assertThat(pair.accessToken()).isEqualTo("new-access");
        verify(refreshTokenRepository).revokeIfActive(anyString(), any());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getFamilyId()).isEqualTo("fam-1");
    }

    @Test
    @DisplayName("refresh: 동시 회전 레이스에서 CAS 패배(0행) 시 재사용으로 간주해 family 폐기 후 401")
    void refresh_lostRotationRace_reuseDetected() {
        RefreshToken current = validToken(1L, "fam-1");
        RefreshToken sibling = validToken(1L, "fam-1");
        given(cacheService.findUserId(anyString())).willReturn(Optional.of(1L));
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(current));
        // 스냅샷은 미폐기지만, 조건부 UPDATE 가 0행 → 다른 동시 요청이 먼저 회전을 소비함.
        given(refreshTokenRepository.revokeIfActive(anyString(), any())).willReturn(0);
        given(refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull("fam-1")).willReturn(List.of(sibling));

        assertThatThrownBy(() -> tokenService.refresh("raw-refresh"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(sibling.isRevoked()).isTrue();
        verify(refreshTokenRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("refresh: 이미 폐기된 토큰 재사용 시 family 전체 폐기 후 401")
    void refresh_reuseDetected_revokesFamily() {
        RefreshToken revoked = validToken(1L, "fam-1");
        revoked.revoke(LocalDateTime.now().minusMinutes(1));
        RefreshToken sibling = validToken(1L, "fam-1");
        given(cacheService.findUserId(anyString())).willReturn(Optional.of(1L));
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(revoked));
        given(refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull("fam-1")).willReturn(List.of(sibling));

        assertThatThrownBy(() -> tokenService.refresh("raw-refresh"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(sibling.isRevoked()).isTrue();
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("refresh: 만료된 토큰이면 폐기 후 ExpiredRefreshTokenException")
    void refresh_expiredToken_throws() {
        RefreshToken expired = RefreshToken.builder()
                .userId(1L).tokenHash("h").familyId("fam-1")
                .issuedAt(LocalDateTime.now().minusDays(20))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        given(cacheService.findUserId(anyString())).willReturn(Optional.empty());
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> tokenService.refresh("raw-refresh"))
                .isInstanceOf(ExpiredRefreshTokenException.class);

        assertThat(expired.isRevoked()).isTrue();
        verify(cacheService).evict(anyString());
    }

    @Test
    @DisplayName("refresh: WITHDRAWN 사용자면 family 폐기 후 401 (매 회전마다 상태 재확인)")
    void refresh_withdrawnUser_rejected() {
        User withdrawn = user(1L, "alice", UserStatus.WITHDRAWN);
        RefreshToken current = validToken(1L, "fam-1");
        given(cacheService.findUserId(anyString())).willReturn(Optional.of(1L));
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(current));
        given(refreshTokenRepository.revokeIfActive(anyString(), any())).willReturn(1);
        given(userRepository.findById(1L)).willReturn(Optional.of(withdrawn));
        given(refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull("fam-1")).willReturn(List.of(current));

        assertThatThrownBy(() -> tokenService.refresh("raw-refresh"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("refresh: 알 수 없는 토큰이면 401")
    void refresh_unknownToken_throws() {
        given(cacheService.findUserId(anyString())).willReturn(Optional.empty());
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.refresh("raw-refresh"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("refresh: Redis 캐시 미스(장애 폴백)여도 DB 경로로 정상 회전")
    void refresh_redisDown_fallsBackToDb() {
        User user = activeUser(1L, "alice");
        RefreshToken current = validToken(1L, "fam-1");
        given(cacheService.findUserId(anyString())).willReturn(Optional.empty()); // Redis 폴백 결과
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(current));
        given(refreshTokenRepository.revokeIfActive(anyString(), any())).willReturn(1);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(anyString(), any(), anyString(), any())).willReturn("new-access");

        TokenPair pair = tokenService.refresh("raw-refresh");

        assertThat(pair.accessToken()).isEqualTo("new-access");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("revoke: 로그아웃 시 DB 폐기 + 캐시 삭제")
    void revoke_marksRevokedAndEvicts() {
        RefreshToken current = validToken(1L, "fam-1");
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(current));

        tokenService.revoke("raw-refresh");

        assertThat(current.isRevoked()).isTrue();
        verify(cacheService).evict(anyString());
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private User activeUser(Long id, String username) {
        return user(id, username, UserStatus.ACTIVE);
    }

    private User user(Long id, String username, UserStatus status) {
        User user = User.builder()
                .username(username)
                .password("{noop}pw")
                .name("이름")
                .nickname(username)
                .status(status)
                .role("ROLE_USER")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private RefreshToken validToken(Long userId, String familyId) {
        return RefreshToken.builder()
                .userId(userId)
                .tokenHash("hash-" + familyId)
                .familyId(familyId)
                .issuedAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build();
    }
}
