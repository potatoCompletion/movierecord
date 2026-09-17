package com.my.movierecord.tmdb.service;

import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.NowPlayingItem;
import com.my.movierecord.tmdb.dto.UpcomingItem;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TmdbHomeService {

    private final TmdbClient tmdbClient;

    // 외부 호출 실패 시의 graceful degrade(빈 목록/null)는 TmdbClient의 resilience4j
    // fallbackMethod에서 처리한다. 여기서는 캐싱과 여러 호출의 조합만 담당한다.
    @Cacheable(value = "nowPlaying", key = "'default'")
    public List<NowPlayingItem> getNowPlaying() {
        return tmdbClient.getNowPlaying();
    }

    /**
     * discover 후보(최대 8건)마다 한국 극장 개봉일을 조회해 조립한다. 하루 1회(캐시 TTL) 만 발생한다.
     * 작품별 release_dates 조회가 실패하면 null 이 돌아와 discover 최초 개봉일로 폴백한다.
     * D-Day 는 저장하지 않고 렌더 시점에 계산한다({@code UpcomingCard}).
     */
    @Cacheable(value = "upcomingMovies", key = "'default'")
    public List<UpcomingItem> getUpcoming() {
        LocalDate today = LocalDate.now();
        return tmdbClient.getUpcoming().stream()
                .map(c -> UpcomingItem.from(c, tmdbClient.getKoreanReleaseDate(c.id(), today)))
                .sorted(Comparator.comparing(UpcomingItem::releaseDate))
                .toList();
    }
}
