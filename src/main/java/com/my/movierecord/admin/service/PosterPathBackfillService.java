package com.my.movierecord.admin.service;

import com.my.movierecord.admin.dto.PosterPathBackfillResult;
import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.movie.domain.ContentId;
import com.my.movierecord.movie.repository.ContentRepository;
import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.TmdbMovieDetail;
import com.my.movierecord.tmdb.dto.TmdbTvDetail;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 기존 콘텐츠의 {@code poster_path} 를 TMDB 상세 API 로 채우는 일회성 백필 서비스.
 *
 * <p><b>임시 코드다.</b> 백필 완료 후 {@code PosterPathBackfillController},
 * {@code PosterPathBackfillResult}, {@code ContentRepository#findAllMissingPosterPath()} 와 함께 제거한다.
 *
 * <ul>
 *   <li><b>멱등성</b>: 조회 쿼리가 poster_path 가 null 인 행만 고르므로 반복 호출해도 채워진 행은 대상에서 빠진다.
 *       루프 안의 null 재확인은 방어 코드일 뿐이며(조회 결과는 detached 스냅숏이라 실제로는 걸리지 않는다) 집계상 skipped 로 센다.</li>
 *   <li><b>실패 허용</b>: TMDB 404(삭제된 콘텐츠) 등 건별 오류는 로그만 남기고 다음 건으로 진행한다.
 *       단, 공용 {@code tmdbApi} 서킷브레이커가 열리면({@link CallNotPermittedException}) 남은 건을 전부 실패로
 *       기록하는 대신 실행을 중단한다. 남은 건은 processed 에 포함되지 않으며 재호출로 이어서 처리한다.</li>
 *   <li><b>트랜잭션</b>: 이 클래스에 {@code @Transactional} 을 붙이지 않는다. 건별
 *       {@link ContentRepository#save(Object)} 가 각자 트랜잭션으로 커밋되므로 중간 실패가
 *       앞서 성공한 건을 롤백하지 않는다.</li>
 *   <li><b>레이트 리밋</b>: TMDB 호출 사이에 {@value #DELAY_BETWEEN_CALLS_MS}ms 대기한다.</li>
 * </ul>
 */
@Service
@Slf4j
public class PosterPathBackfillService {

    static final long DELAY_BETWEEN_CALLS_MS = 100L;

    private static final String MEDIA_TYPE_MOVIE = "movie";
    private static final String MEDIA_TYPE_TV = "tv";

    private final ContentRepository contentRepository;
    private final TmdbClient tmdbClient;

    public PosterPathBackfillService(ContentRepository contentRepository, TmdbClient tmdbClient) {
        this.contentRepository = contentRepository;
        this.tmdbClient = tmdbClient;
    }

    public PosterPathBackfillResult backfill() {
        List<Content> targets = contentRepository.findAllMissingPosterPath();
        log.info("poster_path 백필 시작: 대상 {}건", targets.size());

        int succeeded = 0;
        int skipped = 0;
        List<String> failedIds = new ArrayList<>();

        for (Content content : targets) {
            if (content.getPosterPath() != null) {
                skipped++;
                continue;
            }
            try {
                if (fillOne(content)) {
                    succeeded++;
                } else {
                    failedIds.add(describe(content.getId()));
                }
            } catch (CallNotPermittedException e) {
                log.warn("poster_path 백필 중단 ({}): TMDB 서킷브레이커 open — {}", describe(content.getId()), e.toString());
                break;
            }
            if (!pauseBetweenCalls()) {
                log.warn("poster_path 백필 중단: 스레드 인터럽트");
                break;
            }
        }

        int processed = succeeded + skipped + failedIds.size();
        log.info("poster_path 백필 종료: processed={}, succeeded={}, skipped={}, failed={}",
                processed, succeeded, skipped, failedIds.size());
        return new PosterPathBackfillResult(processed, succeeded, skipped, failedIds.size(),
                List.copyOf(failedIds));
    }

    /**
     * 한 건을 채운다. 건별 오류는 삼키고 실패 여부만 돌려준다.
     * 서킷브레이커 open({@link CallNotPermittedException})만은 전체 중단 판단을 위해 그대로 던진다.
     */
    private boolean fillOne(Content content) {
        ContentId id = content.getId();
        try {
            String posterPath = fetchPosterPath(id);
            if (posterPath == null || posterPath.isBlank()) {
                log.warn("poster_path 백필 실패 ({}): TMDB 응답에 poster_path 없음", describe(id));
                return false;
            }
            content.updatePosterPath(posterPath);
            contentRepository.save(content);
            return true;
        } catch (CallNotPermittedException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("poster_path 백필 실패 ({}): {}", describe(id), e.toString());
            return false;
        }
    }

    private String fetchPosterPath(ContentId id) {
        return switch (id.getMediaType()) {
            case MEDIA_TYPE_MOVIE -> {
                TmdbMovieDetail detail = tmdbClient.getMovieDetail(id.getTmdbId());
                yield detail != null ? detail.posterPath() : null;
            }
            case MEDIA_TYPE_TV -> {
                TmdbTvDetail detail = tmdbClient.getTvDetail(id.getTmdbId());
                yield detail != null ? detail.posterPath() : null;
            }
            default -> throw new IllegalArgumentException("지원하지 않는 media_type: " + id.getMediaType());
        };
    }

    /** 대기에 성공하면 true, 인터럽트되면 플래그를 복원하고 false. */
    private boolean pauseBetweenCalls() {
        try {
            Thread.sleep(DELAY_BETWEEN_CALLS_MS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String describe(ContentId id) {
        return id.getMediaType() + ":" + id.getTmdbId();
    }
}
