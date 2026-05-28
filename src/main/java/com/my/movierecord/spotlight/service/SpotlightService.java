package com.my.movierecord.spotlight.service;

import com.my.movierecord.omdb.client.OmdbClient;
import com.my.movierecord.omdb.dto.OmdbRating;
import com.my.movierecord.spotlight.domain.SpotlightHistory;
import com.my.movierecord.spotlight.dto.SpotlightDto;
import com.my.movierecord.spotlight.repository.SpotlightHistoryRepository;
import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.TmdbDiscoverItem;
import com.my.movierecord.tmdb.dto.TmdbMovieDetail;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotlightService {

    private static final int SLIDE_COUNT    = 3;
    /** 품질 조건을 만족하는 영화를 찾기 위해 페이지를 재시도하는 최대 횟수 */
    private static final int MAX_PAGE_TRIES = 10;
    /** TMDB API 최대 허용 페이지 수 */
    private static final int TMDB_MAX_PAGES = 500;

    private final TmdbClient tmdbClient;
    private final OmdbClient omdbClient;
    private final SpotlightHistoryRepository spotlightHistoryRepository;

    /** 품질 검증을 통과한 영화 데이터 (imdbId, RT 점수 포함) */
    private record MovieWithRatings(TmdbDiscoverItem item, String imdbId, String rtScore) {}

    @Cacheable(value = "todaySpotlight", key = "#date.toString()")
    @Transactional
    public List<SpotlightDto> getSpotlights(LocalDate date) {
        LocalDate today = date;
        Optional<SpotlightHistory> latestOpt = spotlightHistoryRepository.findTopByOrderBySelectedAtDesc();
        SpotlightHistory latest = latestOpt.orElse(null);

        // 오늘 이미 저장된 기록이 있으면 (캐시 eviction 케이스) — 재저장 없이 보조 슬라이드만 추가
        if (latest != null && today.equals(latest.getSelectedAt())) {
            return buildListFromExisting(latest);
        }
        return fetchSaveAndBuildList(today, latest);
    }

    // ── 기존 DB 레코드를 첫 슬라이드로, 나머지는 품질 검증 후 보조 픽업 ─────────
    private List<SpotlightDto> buildListFromExisting(SpotlightHistory primary) {
        List<SpotlightDto> result = new ArrayList<>();
        result.add(SpotlightDto.from(primary));
        try {
            int totalPages = fetchTotalPages();
            Set<Long> usedIds = new HashSet<>();
            usedIds.add(primary.getTmdbId());

            for (int i = 0; i < SLIDE_COUNT - 1; i++) {
                MovieWithRatings mwr = pickOneQualified(totalPages, usedIds);
                if (mwr == null) break;
                result.add(toSpotlightDto(mwr));
            }
        } catch (Exception e) {
            log.warn("Companion spotlight fetch failed: {}", e.getMessage());
        }
        return result;
    }

    // ── 품질 통과 영화 3개 픽, 첫 번째만 DB 저장 ────────────────────────────
    private List<SpotlightDto> fetchSaveAndBuildList(LocalDate today, SpotlightHistory fallback) {
        try {
            int totalPages = fetchTotalPages();
            Set<Long> usedIds = new HashSet<>();
            List<SpotlightDto> result = new ArrayList<>(SLIDE_COUNT);

            for (int i = 0; i < SLIDE_COUNT; i++) {
                MovieWithRatings mwr = pickOneQualified(totalPages, usedIds);
                if (mwr == null) {
                    log.warn("[Spotlight] slot {} 건너뜀: 품질 조건을 만족하는 영화를 찾지 못함", i);
                    break;
                }

                if (i == 0) {
                    // 첫 번째 슬라이드만 DB에 이력 저장
                    SpotlightHistory saved = spotlightHistoryRepository.save(
                            SpotlightHistory.builder()
                                    .tmdbId(mwr.item().id())
                                    .imdbId(mwr.imdbId())
                                    .title(mwr.item().title())
                                    .originalTitle(mwr.item().originalTitle())
                                    .posterPath(mwr.item().posterPath())
                                    .backdropPath(mwr.item().backdropPath())
                                    .releaseYear(toReleaseYear(mwr.item().releaseDate()))
                                    .overview(mwr.item().overview())
                                    .tmdbRating(mwr.item().voteAverage())
                                    .rtScore(mwr.rtScore())
                                    .selectedAt(today)
                                    .build()
                    );
                    result.add(SpotlightDto.from(saved));
                } else {
                    result.add(toSpotlightDto(mwr));
                }
            }

            if (result.isEmpty()) {
                return fallback != null ? List.of(SpotlightDto.from(fallback)) : List.of();
            }
            return result;

        } catch (Exception e) {
            log.warn("Spotlight fetch failed, using fallback: {}", e.getMessage());
            return fallback != null ? List.of(SpotlightDto.from(fallback)) : List.of();
        }
    }

    // ── page 1 호출로 total_pages 확인 ────────────────────────────────────
    private int fetchTotalPages() {
        try {
            int pages = tmdbClient.discoverHighRated(1).totalPages();
            return Math.min(TMDB_MAX_PAGES, Math.max(1, pages));
        } catch (Exception e) {
            log.warn("[Spotlight] total_pages 조회 실패, 기본값 3 사용: {}", e.getMessage());
            return 3;
        }
    }

    /**
     * total_pages 범위에서 랜덤 페이지를 골라 품질 조건을 통과한 영화 한 편을 반환한다.
     *
     * <p>품질 기준 (셋 중 하나 이상 충족):
     * <ul>
     *   <li>IMDb 평점 &gt; 7.5</li>
     *   <li>Rotten Tomatoes &gt; 60%</li>
     *   <li>Metacritic &gt; 75</li>
     * </ul>
     *
     * @param totalPages TMDB discover 응답의 총 페이지 수
     * @param usedIds    이미 선택된 tmdbId 집합 (중복 방지, 이 메서드 내에서 추가됨)
     * @return 품질 통과 영화 데이터, 모든 재시도 실패 시 null
     */
    private MovieWithRatings pickOneQualified(int totalPages, Set<Long> usedIds) {
        for (int attempt = 0; attempt < MAX_PAGE_TRIES; attempt++) {
            int page = ThreadLocalRandom.current().nextInt(1, totalPages + 1);
            List<TmdbDiscoverItem> pool;
            try {
                pool = tmdbClient.discoverHighRated(page).results();
            } catch (Exception e) {
                log.warn("[Spotlight] discoverHighRated page={} 실패 (attempt {}): {}",
                        page, attempt + 1, e.getMessage());
                continue;
            }

            List<TmdbDiscoverItem> shuffled = new ArrayList<>(pool);
            Collections.shuffle(shuffled, ThreadLocalRandom.current());

            for (TmdbDiscoverItem item : shuffled) {
                if (!usedIds.add(item.id())) continue; // 이미 선택된 영화 건너뜀

                String imdbId = fetchImdbId(item.id());
                List<OmdbRating> ratings = imdbId != null ? fetchAllRatings(imdbId) : List.of();

                if (meetsQualityThreshold(ratings)) {
                    log.debug("[Spotlight] 품질 통과: id={} title='{}'", item.id(), item.title());
                    return new MovieWithRatings(item, imdbId, extractRtScore(ratings));
                }
                log.debug("[Spotlight] 품질 미달로 제외: id={} title='{}'", item.id(), item.title());
            }
        }
        log.warn("[Spotlight] {}번 시도 후 품질 조건을 만족하는 영화를 찾지 못함", MAX_PAGE_TRIES);
        return null;
    }

    // ── MovieWithRatings → SpotlightDto 변환 ──────────────────────────────
    private static SpotlightDto toSpotlightDto(MovieWithRatings mwr) {
        TmdbDiscoverItem item = mwr.item();
        return new SpotlightDto(
                item.id(), item.title(), item.originalTitle(),
                item.posterPath(), item.backdropPath(),
                toReleaseYear(item.releaseDate()), item.overview(),
                item.voteAverage(), mwr.rtScore()
        );
    }

    // ── OMDB 전체 평점 목록 조회 ───────────────────────────────────────────
    private List<OmdbRating> fetchAllRatings(String imdbId) {
        try {
            return omdbClient.getRatings(imdbId);
        } catch (Exception e) {
            log.warn("[Spotlight] OMDB ratings 조회 실패 imdbId={}: {}", imdbId, e.getMessage());
            return List.of();
        }
    }

    /**
     * IMDb &gt; 7.5 OR Rotten Tomatoes &gt; 60 OR Metacritic &gt; 75 중 하나 이상 충족 시 true.
     * 세 기관 모두 점수가 없으면 false (= 다시 픽 대상).
     */
    private static boolean meetsQualityThreshold(List<OmdbRating> ratings) {
        for (OmdbRating r : ratings) {
            switch (r.source()) {
                case "IMDb" -> {
                    try {
                        double score = Double.parseDouble(r.value().split("/")[0].trim());
                        if (score > 7.5) return true;
                    } catch (NumberFormatException ignored) {}
                }
                case "Rotten Tomatoes" -> {
                    try {
                        int score = Integer.parseInt(r.value().replace("%", "").trim());
                        if (score > 60) return true;
                    } catch (NumberFormatException ignored) {}
                }
                case "Metacritic" -> {
                    try {
                        int score = Integer.parseInt(r.value().split("/")[0].trim());
                        if (score > 75) return true;
                    } catch (NumberFormatException ignored) {}
                }
                default -> {}
            }
        }
        return false;
    }

    // ── 평점 목록에서 Rotten Tomatoes 점수만 추출 ─────────────────────────
    private static String extractRtScore(List<OmdbRating> ratings) {
        return ratings.stream()
                .filter(r -> "Rotten Tomatoes".equals(r.source()))
                .map(OmdbRating::value)
                .findFirst()
                .orElse(null);
    }

    private String fetchImdbId(Long tmdbId) {
        try {
            TmdbMovieDetail detail = tmdbClient.getMovieDetail(tmdbId);
            return detail != null ? detail.imdbId() : null;
        } catch (Exception e) {
            log.warn("[Spotlight] TMDB detail 조회 실패 id={}: {}", tmdbId, e.getMessage());
            return null;
        }
    }

    private static String toReleaseYear(String releaseDate) {
        return releaseDate != null && releaseDate.length() >= 4
                ? releaseDate.substring(0, 4) : null;
    }
}
