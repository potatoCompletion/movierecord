package com.my.movierecord.auth.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 리프레시 토큰 해시 → userId 조회 캐시(Redis).
 *
 * <p>DB(`refresh_tokens`)가 원장(source of truth)이고 Redis는 조회 가속용 캐시일 뿐이다.
 * 모든 Redis 접근은 resilience4j {@code refreshTokenCache} 서킷브레이커로 보호된다. 여기서는
 * fallback 을 두지 않아 <b>실패가 서킷브레이커에 집계되도록</b> 예외를 그대로 전파한다
 * (fallbackMethod 를 서킷브레이커에 붙이면 예외가 성공으로 기록되어 서킷이 열리지 않는다 —
 * 기존 {@code TmdbClient}가 fallback 을 {@code @Retry}에만 붙이는 이유와 동일).
 * 장애 시 DB 로 폴백하는 graceful degradation 은 호출부({@link TokenService})가 try/catch 로 담당한다.
 */
@Service
public class RefreshTokenCacheService {

    private static final String INSTANCE = "refreshTokenCache";
    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @CircuitBreaker(name = INSTANCE)
    public Optional<Long> findUserId(String tokenHash) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + tokenHash);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @CircuitBreaker(name = INSTANCE)
    public void put(String tokenHash, Long userId, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + tokenHash, String.valueOf(userId), ttl);
    }

    @CircuitBreaker(name = INSTANCE)
    public void evict(String tokenHash) {
        redisTemplate.delete(KEY_PREFIX + tokenHash);
    }
}
