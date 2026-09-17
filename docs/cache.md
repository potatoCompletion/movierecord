# 캐시 전략

> [← README](../README.md)

## Spring Cache + Redis (운영 전용)

Spring Cache 추상화 위에 Redis를 백엔드로 사용합니다. `@Cacheable` 어노테이션만으로 캐시를 적용할 수 있어 도메인별로 점진적으로 확장합니다. 설정은 `config/CacheConfig`에 있습니다.

## TTL 설정

캐시 이름별로 TTL을 개별 지정하고, 별도 설정이 없는 캐시는 기본 TTL을 따릅니다.

| 캐시 이름 | 대상 | 키 | TTL |
|----------|------|-----|-----|
| `todaySpotlight` | 홈 스포트라이트 3편 | 날짜(`LocalDate`) | 1일 (전날 23:00 스케줄러가 사전 워밍) |
| `dailyBoxOffice` | KOBIS 일별 박스오피스 TOP 10 | `LocalDate.now().minusDays(1)` | 1일 |
| `nowPlaying`, `upcomingMovies` | TMDB 현재 상영·개봉 예정 | 고정 키 | 1일 |
| 기본 | 개별 TTL을 지정하지 않은 캐시 | 메서드별 | 1시간 |

리프레시 토큰 조회 캐시는 Spring Cache가 아니라 `StringRedisTemplate`으로 직접 다루며, 키마다 리프레시 토큰 만료 시각과 같은 TTL을 둡니다. 장애 시 DB 원장으로 폴백하는 구조는 [auth.md](auth.md)에 있습니다.

## 스포트라이트 사전 워밍

`SpotlightScheduler`가 매일 23:00(`cron = "0 0 23 * * *"`)에 익일 날짜로 `SpotlightService.getSpotlights()`를 호출합니다. 캐시 키가 날짜 기반이므로 자정 이후 첫 요청에서 cache miss → 신규 픽(OMDb 평점 검증, 최대 10회 재시도)이 일어나기 전에 스케줄러가 미리 채워 둡니다. 자정 직후 접속한 사용자도 지연 없이 응답을 받습니다.

## 직렬화와 캐시 키 버저닝

`GenericJacksonJsonRedisSerializer`로 값을 순수 JSON으로 저장합니다. `enableDefaultTyping`을 쓰지 않아 JSON에 `@class` 타입 정보가 들어가지 않으므로, 클래스 경로가 바뀌어도 기존 캐시 데이터를 역직렬화할 수 있다는 장점이 있지만, **캐시된 값의 구조나 의미가 바뀌면 배포 직후 옛 캐시가 그대로 서빙된다**는 문제가 따라옵니다. TTL이 만료될 때까지 새 코드가 옛 형식의 데이터를 읽게 됩니다.

캐시 키에 버전 접두사를 붙여 해결했습니다. 배포 시 버전만 올리면 애플리케이션이 새 키를 조회하므로 옛 캐시를 자연스럽게 우회하고, 남은 키는 TTL로 소멸합니다.

```java
// CacheConfig.java
// 캐시된 값의 구조 또는 의미가 바뀌면 이 값을 올린다
private static final String CACHE_VERSION = "v4";

RedisCacheConfiguration.defaultCacheConfig()
        .computePrefixWith(CacheKeyPrefix.prefixed(CACHE_VERSION + ":"))
```

키는 `v4:nowPlaying::default` 형태가 됩니다. `@Cacheable` 선언을 수정하지 않고 설정 한 곳에서 전체 캐시를 무효화할 수 있습니다. 버전을 올린 이력은 다음과 같습니다.

| 버전 | 계기 |
|------|------|
| v3 | 포스터 URL을 `/uploads/` 경로에서 TMDB CDN URL로 전환. 필드 구조는 같지만 값의 의미가 바뀜 |
| v4 | "곧 개봉해요" 캐시 항목에서 사전 계산 D-Day를 제거하고 한국 개봉일·재개봉 플래그를 저장하도록 구조 변경 |

`upcomingMovies` 캐시만은 예외적으로 타입을 고정한 `JacksonJsonRedisSerializer<List<UpcomingItem>>`를 씁니다. 컨트롤러가 이 값을 `UpcomingItem`으로 읽어 D-Day를 계산하므로 `LinkedHashMap`으로 복원되면 안 되기 때문입니다.

## 로컬 환경

`local` 프로파일은 `spring.cache.type=simple`로 Spring의 기본 `ConcurrentMapCacheManager`를 사용하고, Redis용 `RedisCacheManager` 빈은 `prod` 프로파일에서만 등록됩니다. Redis 없이 로컬 개발이 가능합니다. 테스트도 같은 이유로 Redis 없이 돌아갑니다.
