package com.my.movierecord.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/**
 * 액세스 토큰(JWT, HS256) 발급/검증.
 *
 * <p>서명 시크릿은 {@code app.jwt.secret} 환경변수로만 주입한다(하드코딩 금지).
 * 누락되거나 32바이트 미만이면 기동 시 예외로 fail-fast 한다.
 */
@Component
@Slf4j
public class JwtProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_NICKNAME = "nickname";

    private final SecretKey key;
    private final long accessTokenTtlSeconds;

    public JwtProvider(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 bytes for HS256 (was " + keyBytes.length + ")");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public Duration accessTokenTtl() {
        return Duration.ofSeconds(accessTokenTtlSeconds);
    }

    public String createAccessToken(String username, Long userId, String role, String nickname) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenTtlSeconds * 1000);
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_NICKNAME, nickname)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 서명·만료가 유효하면 클레임을 반환하고, 그 외(만료/변조/형식오류)에는 빈 값을 반환한다. */
    public Optional<AccessTokenClaims> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = claims.get(CLAIM_USER_ID, Number.class) instanceof Number n ? n.longValue() : null;
            return Optional.of(new AccessTokenClaims(
                    claims.getSubject(),
                    userId,
                    claims.get(CLAIM_ROLE, String.class),
                    claims.get(CLAIM_NICKNAME, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("액세스 토큰 검증 실패: {}", e.toString());
            return Optional.empty();
        }
    }

    public record AccessTokenClaims(String username, Long userId, String role, String nickname) {
    }
}
