package com.my.movierecord.auth.service;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.dto.TokenPair;
import com.my.movierecord.auth.enums.UserStatus;
import com.my.movierecord.auth.exception.InvalidRefreshTokenException;
import com.my.movierecord.auth.repository.RefreshTokenRepository;
import com.my.movierecord.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 리프레시 토큰 회전의 one-time-use 보장을 실제 동시성으로 검증한다(H1 TOCTOU 레이스 회귀 방지).
 * 동일 유효 리프레시 토큰으로 두 요청이 동시에 들어와도 정확히 하나만 회전에 성공하고,
 * 나머지는 재사용으로 간주되어 실패해야 한다.
 *
 * <p>Redis는 no-op 목으로 대체해 캐시 접근이 테스트에 개입하지 않도록 한다(원장은 DB).
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
class TokenServiceConcurrencyTest {

    @TestConfiguration
    static class NoOpRedisConfig {
        @Bean
        StringRedisTemplate stringRedisTemplate() {
            StringRedisTemplate template = mock(StringRedisTemplate.class);
            @SuppressWarnings("unchecked")
            ValueOperations<String, String> valueOps = mock(ValueOperations.class);
            given(template.opsForValue()).willReturn(valueOps); // get→null(캐시 미스), set→no-op
            return template;
        }
    }

    @Autowired
    TokenService tokenService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    private Long createdUserId;

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        if (createdUserId != null) {
            userRepository.findById(createdUserId).ifPresent(userRepository::delete);
        }
    }

    @Test
    @DisplayName("동일 리프레시 토큰으로 동시 회전 시 정확히 하나만 성공하고 하나는 재사용으로 실패")
    void concurrentRefresh_onlyOneWins() throws Exception {
        User user = userRepository.save(User.builder()
                .username("race-user")
                .password("{noop}pw")
                .name("레이스")
                .nickname("race-user")
                .status(UserStatus.ACTIVE)
                .role("ROLE_USER")
                .build());
        createdUserId = user.getId();

        TokenPair initial = tokenService.issueTokenPair(user);
        String rawRefresh = initial.refreshToken();

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<TokenPair>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return tokenService.refresh(rawRefresh);
            }));
        }
        ready.await();
        go.countDown();

        int success = 0;
        int reuseFailures = 0;
        for (Future<TokenPair> future : futures) {
            try {
                assertThat(future.get().accessToken()).isNotBlank();
                success++;
            } catch (ExecutionException e) {
                assertThat(e.getCause()).isInstanceOf(InvalidRefreshTokenException.class);
                reuseFailures++;
            }
        }
        pool.shutdownNow();

        assertThat(success).as("정확히 하나의 회전만 성공해야 한다").isEqualTo(1);
        assertThat(reuseFailures).as("나머지 하나는 재사용으로 실패해야 한다").isEqualTo(1);
        // 초기 토큰 1 + 승자가 발급한 신규 토큰 1 = 2. 패자는 신규 토큰을 만들지 않는다.
        assertThat(refreshTokenRepository.count()).isEqualTo(2);
    }
}
