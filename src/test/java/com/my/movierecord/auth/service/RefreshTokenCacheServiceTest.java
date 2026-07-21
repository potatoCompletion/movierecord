package com.my.movierecord.auth.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Redis 리프레시 캐시의 resilience4j 서킷브레이커 + DB 폴백 계약 검증.
 * StringRedisTemplate 을 항상 예외를 던지는 목으로 오버라이드해, Redis 장애 시에도
 * 조회가 fallback(빈 값)으로 degrade 되고 서킷이 OPEN 으로 전이됨을 확인한다.
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
class RefreshTokenCacheServiceTest {

    @TestConfiguration
    static class FailingRedisConfig {
        @Bean
        StringRedisTemplate stringRedisTemplate() {
            StringRedisTemplate template = mock(StringRedisTemplate.class);
            @SuppressWarnings("unchecked")
            ValueOperations<String, String> valueOps = mock(ValueOperations.class);
            given(template.opsForValue()).willReturn(valueOps);
            given(valueOps.get(anyString())).willThrow(new RuntimeException("simulated redis outage"));
            return template;
        }
    }

    @Autowired
    RefreshTokenCacheService cacheService;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetBreaker() {
        circuitBreakerRegistry.circuitBreaker("refreshTokenCache").reset();
    }

    @Test
    @DisplayName("Redis 장애 시 조회는 예외를 전파해 서킷브레이커에 실패로 집계된다(호출부가 DB로 폴백)")
    void findUserId_redisDown_propagatesForBreaker() {
        assertThatThrownBy(() -> cacheService.findUserId("some-hash"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("반복 실패 시 refreshTokenCache 서킷이 CLOSED → OPEN 으로 전이된다")
    void repeatedFailures_openCircuit() {
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("refreshTokenCache");
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        for (int i = 0; i < 10; i++) {
            try {
                cacheService.findUserId("hash-" + i);
            } catch (Exception ignored) {
                // Redis 장애/서킷 오픈 예외는 호출부(TokenService)가 삼키고 DB 로 폴백한다.
            }
        }

        assertThat(breaker.getState())
                .isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.FORCED_OPEN);
        assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isGreaterThan(0);
    }
}
