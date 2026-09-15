package com.my.movierecord.admin.service;

import com.my.movierecord.admin.dto.PosterPathBackfillResult;
import com.my.movierecord.common.exception.ExternalApiClientException;
import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.movie.repository.ContentRepository;
import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.TmdbMovieDetail;
import com.my.movierecord.tmdb.dto.TmdbTvDetail;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PosterPathBackfillServiceTest {

    private static final TmdbImageUrlProvider IMAGES = new TmdbImageUrlProvider("https://image.tmdb.org/t/p");

    @Mock
    ContentRepository contentRepository;

    @Mock
    TmdbClient tmdbClient;

    @InjectMocks
    PosterPathBackfillService service;

    @Test
    void backfill_movie와_tv를_각각의_TMDB_상세로_채우고_건별로_저장한다() {
        Content movie = Content.of(1L, "movie");
        Content tv = Content.of(2L, "tv");
        given(contentRepository.findAllMissingPosterPath()).willReturn(List.of(movie, tv));
        given(tmdbClient.getMovieDetail(1L)).willReturn(movieDetail(1L, "/movie.jpg"));
        given(tmdbClient.getTvDetail(2L)).willReturn(tvDetail(2L, "/tv.jpg"));

        PosterPathBackfillResult result = service.backfill();

        assertThat(result.processed()).isEqualTo(2);
        assertThat(result.succeeded()).isEqualTo(2);
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(result.failedIds()).isEmpty();
        assertThat(movie.getPosterPath()).isEqualTo("/movie.jpg");
        assertThat(tv.getPosterPath()).isEqualTo("/tv.jpg");
        then(contentRepository).should().save(movie);
        then(contentRepository).should().save(tv);
        then(tmdbClient).should(never()).getTvDetail(1L);
        then(tmdbClient).should(never()).getMovieDetail(2L);
    }

    @Test
    void backfill_TMDB_404는_건너뛰고_로그만_남기며_다음_건은_계속_처리한다() {
        Content deleted = Content.of(10L, "movie");
        Content alive = Content.of(11L, "movie");
        given(contentRepository.findAllMissingPosterPath()).willReturn(List.of(deleted, alive));
        given(tmdbClient.getMovieDetail(10L))
                .willThrow(new ExternalApiClientException("tmdb", 404, "tmdb API returned HTTP 404"));
        given(tmdbClient.getMovieDetail(11L)).willReturn(movieDetail(11L, "/alive.jpg"));

        PosterPathBackfillResult result = service.backfill();

        assertThat(result.processed()).isEqualTo(2);
        assertThat(result.succeeded()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.failedIds()).containsExactly("movie:10");
        assertThat(deleted.getPosterPath()).isNull();
        assertThat(alive.getPosterPath()).isEqualTo("/alive.jpg");
        then(contentRepository).should(never()).save(deleted);
        then(contentRepository).should().save(alive);
    }

    @Test
    void backfill_서킷브레이커가_열리면_남은_건을_실패로_세지_않고_중단한다() {
        Content first = Content.of(40L, "movie");
        Content tripped = Content.of(41L, "movie");
        Content remaining = Content.of(42L, "movie");
        given(contentRepository.findAllMissingPosterPath()).willReturn(List.of(first, tripped, remaining));
        given(tmdbClient.getMovieDetail(40L)).willReturn(movieDetail(40L, "/first.jpg"));
        given(tmdbClient.getMovieDetail(41L)).willThrow(
                CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("tmdbApi")));

        PosterPathBackfillResult result = service.backfill();

        assertThat(result).isEqualTo(new PosterPathBackfillResult(1, 1, 0, 0, List.of()));
        then(tmdbClient).should(never()).getMovieDetail(42L);
        then(contentRepository).should().save(first);
        then(contentRepository).should(never()).save(tripped);
        then(contentRepository).should(never()).save(remaining);
    }

    @Test
    void backfill_이미_채워진_건은_TMDB를_호출하지_않고_건너뛴다() {
        Content filled = Content.of(20L, "tv");
        filled.updatePosterPath("/already.jpg");
        given(contentRepository.findAllMissingPosterPath()).willReturn(List.of(filled));

        PosterPathBackfillResult result = service.backfill();

        assertThat(result.processed()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.succeeded()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(filled.getPosterPath()).isEqualTo("/already.jpg");
        then(tmdbClient).shouldHaveNoInteractions();
        then(contentRepository).should(never()).save(any());
    }

    @Test
    void backfill_TMDB_응답에_poster_path가_없으면_실패로_집계하고_저장하지_않는다() {
        Content noPoster = Content.of(30L, "tv");
        given(contentRepository.findAllMissingPosterPath()).willReturn(List.of(noPoster));
        given(tmdbClient.getTvDetail(30L)).willReturn(tvDetail(30L, null));

        PosterPathBackfillResult result = service.backfill();

        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.failedIds()).containsExactly("tv:30");
        assertThat(noPoster.getPosterPath()).isNull();
        then(contentRepository).should(never()).save(any());
    }

    @Test
    void backfill_대상이_없으면_TMDB를_호출하지_않고_0건을_반환한다() {
        given(contentRepository.findAllMissingPosterPath()).willReturn(List.of());

        PosterPathBackfillResult result = service.backfill();

        assertThat(result).isEqualTo(new PosterPathBackfillResult(0, 0, 0, 0, List.of()));
        then(tmdbClient).shouldHaveNoInteractions();
        then(contentRepository).should(times(0)).save(any());
    }

    private static TmdbMovieDetail movieDetail(Long id, String posterPath) {
        Map<String, Object> raw = new HashMap<>();
        raw.put("id", id);
        raw.put("poster_path", posterPath);
        return TmdbMovieDetail.from(raw, IMAGES);
    }

    private static TmdbTvDetail tvDetail(Long id, String posterPath) {
        Map<String, Object> raw = new HashMap<>();
        raw.put("id", id);
        raw.put("poster_path", posterPath);
        return TmdbTvDetail.from(raw, IMAGES);
    }
}
