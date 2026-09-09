package com.my.movierecord.tmdb.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TmdbImageUrlProviderTest {

    private static final String BASE = "https://image.tmdb.org/t/p";

    @Test
    @DisplayName("base + 사이즈 + 경로를 슬래시 하나로 이어 붙인다")
    void poster_buildsUrl() {
        TmdbImageUrlProvider provider = new TmdbImageUrlProvider(BASE);

        assertThat(provider.poster("/abc.jpg", PosterSize.W500))
                .isEqualTo("https://image.tmdb.org/t/p/w500/abc.jpg");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("경로가 null/blank 면 null 을 돌려준다")
    void poster_nullOrBlankPath_returnsNull(String path) {
        TmdbImageUrlProvider provider = new TmdbImageUrlProvider(BASE);

        assertThat(provider.poster(path, PosterSize.W500)).isNull();
        assertThat(provider.backdrop(path, PosterSize.W1280)).isNull();
        assertThat(provider.profile(path, PosterSize.W342)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "https://image.tmdb.org/t/p,  /abc.jpg",
        "https://image.tmdb.org/t/p/, /abc.jpg",
        "https://image.tmdb.org/t/p,  abc.jpg",
        "https://image.tmdb.org/t/p/, abc.jpg"
    })
    @DisplayName("base 끝 슬래시와 경로 앞 슬래시 유무에 관계없이 슬래시가 중복되지 않는다")
    void poster_noDuplicateSlash(String base, String path) {
        TmdbImageUrlProvider provider = new TmdbImageUrlProvider(base);

        assertThat(provider.poster(path, PosterSize.W185))
                .isEqualTo("https://image.tmdb.org/t/p/w185/abc.jpg");
    }

    @ParameterizedTest
    @CsvSource({
        "W185,  w185",
        "W342,  w342",
        "W500,  w500",
        "W1280, w1280"
    })
    @DisplayName("사이즈 enum 값이 경로 세그먼트로 들어간다")
    void sizes(PosterSize size, String segment) {
        TmdbImageUrlProvider provider = new TmdbImageUrlProvider(BASE);

        assertThat(provider.backdrop("/x.jpg", size))
                .isEqualTo("https://image.tmdb.org/t/p/" + segment + "/x.jpg");
    }
}
