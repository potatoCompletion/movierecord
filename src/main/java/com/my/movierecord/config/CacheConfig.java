package com.my.movierecord.config;

import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@EnableCaching
@Configuration
public class CacheConfig {

    /**
     * 캐시 키 버전 접두사. 캐시 값은 타입 정보 없이 JSON으로 저장되어 LinkedHashMap 으로 복원되므로,
     * 캐시된 값의 구조 또는 의미가 바뀌면 이 값을 올려 옛 캐시를 무효화한다.
     * 필드 구성이 그대로여도 값의 형식이나 출처가 바뀌면 대상이다 (예: 포스터 URL 이 /uploads/ 경로에서 TMDB CDN URL 로 변경).
     * 올리지 않으면 TTL 만료 전까지 옛 값이 그대로 서빙된다.
     */
    private static final String CACHE_VERSION = "v3";

    @Bean
    @Profile("prod")
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder().build();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .computePrefixWith(CacheKeyPrefix.prefixed(CACHE_VERSION + ":"))
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        RedisCacheConfiguration oneDayConfig = config.entryTtl(Duration.ofDays(1));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("dailyBoxOffice", oneDayConfig)
                .withCacheConfiguration("nowPlaying", oneDayConfig)
                .withCacheConfiguration("upcomingMovies", oneDayConfig)
                .withCacheConfiguration("todaySpotlight", oneDayConfig)
                .build();
    }
}
