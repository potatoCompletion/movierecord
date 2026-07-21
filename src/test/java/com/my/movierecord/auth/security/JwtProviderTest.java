package com.my.movierecord.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-for-jwt-signing-1234567890";

    private final JwtProvider provider = new JwtProvider(SECRET, 900);

    @Test
    @DisplayName("발급한 액세스 토큰을 파싱하면 클레임이 복원된다")
    void createAndParse_roundTrip() {
        String token = provider.createAccessToken("alice", 7L, "ROLE_USER", "앨리스");

        Optional<JwtProvider.AccessTokenClaims> parsed = provider.parseAccessToken(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().username()).isEqualTo("alice");
        assertThat(parsed.get().userId()).isEqualTo(7L);
        assertThat(parsed.get().role()).isEqualTo("ROLE_USER");
        assertThat(parsed.get().nickname()).isEqualTo("앨리스");
    }

    @Test
    @DisplayName("만료된 토큰은 파싱 시 빈 값을 반환한다")
    void parse_expiredToken_returnsEmpty() {
        JwtProvider expiredProvider = new JwtProvider(SECRET, -1);
        String expired = expiredProvider.createAccessToken("bob", 1L, "ROLE_USER", "밥");

        assertThat(provider.parseAccessToken(expired)).isEmpty();
    }

    @Test
    @DisplayName("변조된 토큰은 파싱 시 빈 값을 반환한다")
    void parse_tamperedToken_returnsEmpty() {
        String token = provider.createAccessToken("carol", 2L, "ROLE_USER", "캐롤");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("a") ? "bb" : "aa");

        assertThat(provider.parseAccessToken(tampered)).isEmpty();
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰은 검증에 실패한다")
    void parse_wrongSignature_returnsEmpty() {
        JwtProvider other = new JwtProvider("another-secret-key-totally-different-9876", 900);
        String token = other.createAccessToken("dave", 3L, "ROLE_USER", "데이브");

        assertThat(provider.parseAccessToken(token)).isEmpty();
    }

    @Test
    @DisplayName("시크릿이 32바이트 미만이면 생성 시 예외로 fail-fast")
    void constructor_shortSecret_throws() {
        assertThatThrownBy(() -> new JwtProvider("too-short", 900))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
