package com.my.movierecord.config;

import com.my.movierecord.tmdb.dto.UpcomingItem;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@EnableCaching
@Configuration
public class CacheConfig {

    /**
     * 캐시 키 버전 접두사. 캐시 값은 타입 정보 없이 JSON으로 저장되어 LinkedHashMap 으로 복원되므로,
     * 캐시된 값의 구조 또는 의미가 바뀌면 이 값을 올려 옛 캐시를 무효화한다.
     * 필드 구성이 그대로여도 값의 형식이나 출처가 바뀌면 대상이다 (예: 포스터 URL 이 /uploads/ 경로에서 TMDB CDN URL 로 변경).
     * 올리지 않으면 TTL 만료 전까지 옛 값이 그대로 서빙된다.
     * 예외로 upcomingMovies 는 Java 코드가 값을 UpcomingItem 타입으로 읽으므로 전용 타입 직렬화기를 쓴다.
     */
    private static final String CACHE_VERSION = "v4";

    @Bean
    @Profile("prod")
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder().build();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .computePrefixWith(CacheKeyPrefix.prefixed(CACHE_VERSION + ":"))
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        RedisCacheConfiguration oneDayConfig = config.entryTtl(Duration.ofDays(1));

        // 타입 정보 없는 JSON 은 LinkedHashMap 으로 복원되므로, 컨트롤러가 필드를 읽는 이 캐시만 타입을 고정한다.
        JacksonJsonRedisSerializer<List<UpcomingItem>> upcomingSerializer = new JacksonJsonRedisSerializer<>(
                objectMapper, objectMapper.getTypeFactory().constructCollectionType(List.class, UpcomingItem.class));
        RedisCacheConfiguration upcomingConfig = oneDayConfig
                .serializeValuesWith(SerializationPair.fromSerializer(upcomingSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("dailyBoxOffice", oneDayConfig)
                .withCacheConfiguration("nowPlaying", oneDayConfig)
                .withCacheConfiguration("upcomingMovies", upcomingConfig)
                .withCacheConfiguration("todaySpotlight", oneDayConfig)
                .build();
    }
}
