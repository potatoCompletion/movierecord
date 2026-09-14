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
     * DTO 구조가 바뀌면 이 값을 올려 옛 캐시를 무효화한다.
     */
    private static final String CACHE_VERSION = "v2";

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
