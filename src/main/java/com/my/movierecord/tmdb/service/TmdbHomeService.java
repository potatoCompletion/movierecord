package com.my.movierecord.tmdb.service;

import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.NowPlayingItem;
import com.my.movierecord.tmdb.dto.UpcomingItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TmdbHomeService {

    private final TmdbClient tmdbClient;

    // 외부 호출 실패 시의 graceful degrade(빈 목록)는 TmdbClient의 resilience4j
    // fallbackMethod에서 처리한다. 여기서는 캐싱만 담당한다.
    @Cacheable(value = "nowPlaying", key = "'default'")
    public List<NowPlayingItem> getNowPlaying() {
        return tmdbClient.getNowPlaying();
    }

    @Cacheable(value = "upcomingMovies", key = "'default'")
    public List<UpcomingItem> getUpcoming() {
        return tmdbClient.getUpcoming();
    }
}
