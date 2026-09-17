package com.my.movierecord.config;

import com.my.movierecord.tmdb.dto.UpcomingItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * RedisCacheManager 는 build/initializeCaches 시점에 Redis 에 접속하지 않으므로
 * mock RedisConnectionFactory 로 설정만 검증한다.
 */
class CacheConfigTest {

    private static final String CACHE_VERSION_PREFIX = "v4:";

    private RedisCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new CacheConfig().cacheManager(mock(RedisConnectionFactory.class), JsonMapper.builder().build());
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
    void upcomingMovies_값은_타입_고정_직렬화로_UpcomingItem_인스턴스로_복원된다() {
        SerializationPair<Object> values = cacheManager.getCacheConfigurations()
                .get("upcomingMovies").getValueSerializationPair();
        List<UpcomingItem> original = List.of(
                new UpcomingItem(1L, "제목", "Title", "/p.jpg", "https://img/p.jpg", "2026-10-01", true));

        Object restored = values.read(values.write(original));

        assertThat(restored).isInstanceOf(List.class);
        assertThat((List<?>) restored).singleElement().isInstanceOf(UpcomingItem.class).isEqualTo(original.get(0));
    }

    @Test
    void 다른_캐시는_타입_정보_없는_JSON으로_저장돼_Map으로_복원된다() {
        SerializationPair<Object> values = cacheManager.getCacheConfigurations()
                .get("nowPlaying").getValueSerializationPair();
        List<UpcomingItem> original = List.of(
                new UpcomingItem(1L, "제목", "Title", "/p.jpg", "https://img/p.jpg", "2026-10-01", true));

        Object restored = values.read(values.write(original));

        assertThat((List<?>) restored).singleElement().isInstanceOf(Map.class);
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
