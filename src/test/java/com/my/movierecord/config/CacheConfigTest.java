package com.my.movierecord.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * RedisCacheManager 는 build/initializeCaches 시점에 Redis 에 접속하지 않으므로
 * mock RedisConnectionFactory 로 설정만 검증한다.
 */
class CacheConfigTest {

    private static final String CACHE_VERSION_PREFIX = "v2:";

    private RedisCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new CacheConfig().cacheManager(mock(RedisConnectionFactory.class));
        // builder().build() 는 캐시를 초기화하지 않으므로 명시적으로 초기화해야 getCacheConfigurations() 가 채워진다.
        cacheManager.initializeCaches();
    }

    @ParameterizedTest
    @ValueSource(strings = {"dailyBoxOffice", "nowPlaying", "upcomingMovies", "todaySpotlight"})
    void 개별_설정된_캐시는_버전_접두사가_붙은_키를_쓴다(String cacheName) {
        Map<String, RedisCacheConfiguration> configs = cacheManager.getCacheConfigurations();

        assertThat(configs).containsKey(cacheName);
        assertThat(configs.get(cacheName).getKeyPrefixFor(cacheName))
                .isEqualTo(CACHE_VERSION_PREFIX + cacheName + "::");
    }

    @Test
    void 기본_설정으로_생성되는_캐시도_버전_접두사가_붙은_키를_쓴다() {
        String cacheName = "unregisteredCache";

        RedisCache cache = (RedisCache) cacheManager.getCache(cacheName);

        assertThat(cache).isNotNull();
        assertThat(cache.getCacheConfiguration().getKeyPrefixFor(cacheName))
                .isEqualTo(CACHE_VERSION_PREFIX + cacheName + "::");
    }
}
