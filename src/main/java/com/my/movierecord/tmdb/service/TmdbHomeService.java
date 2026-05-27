package com.my.movierecord.tmdb.service;

import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.NowPlayingItem;
import com.my.movierecord.tmdb.dto.UpcomingItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TmdbHomeService {

    private final TmdbClient tmdbClient;

    @Cacheable(value = "nowPlaying", key = "'default'")
    public List<NowPlayingItem> getNowPlaying() {
        try {
            return tmdbClient.getNowPlaying();
        } catch (Exception e) {
            log.warn("TMDB now playing fetch failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Cacheable(value = "upcomingMovies", key = "'default'")
    public List<UpcomingItem> getUpcoming() {
        try {
            return tmdbClient.getUpcoming();
        } catch (Exception e) {
            log.warn("TMDB upcoming fetch failed: {}", e.getMessage());
            return List.of();
        }
    }
}
