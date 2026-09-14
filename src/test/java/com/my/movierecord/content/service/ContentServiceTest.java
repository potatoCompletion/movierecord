package com.my.movierecord.content.service;

import com.my.movierecord.common.service.FileStorageService;
import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.movie.domain.ContentId;
import com.my.movierecord.movie.repository.ContentRepository;
import com.my.movierecord.movie.service.ContentService;
import com.my.movierecord.tmdb.client.TmdbImageClient;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    ContentRepository contentRepository;

    @Mock
    FileStorageService fileStorageService;

    @Mock
    TmdbImageClient tmdbImageClient;

    @InjectMocks
    ContentService contentService;

    @Test
    void findOrCreate_기존_레코드가_있으면_그대로_반환하고_다운로드하지_않는다() {
        Content existing = Content.of(1L, "movie");
        given(contentRepository.findById(ContentId.of(1L, "movie"))).willReturn(Optional.of(existing));

        Content result = contentService.findOrCreate(1L, "movie", "/abc123.jpg");

        assertThat(result).isSameAs(existing);
        assertThat(result.getPosterPath()).isNull();
        then(tmdbImageClient).shouldHaveNoInteractions();
        then(contentRepository).should(never()).save(any());
    }

    @Test
    void findOrCreate_신규_레코드는_posterPath와_thumbnailPath를_함께_저장한다() {
        given(contentRepository.findById(ContentId.of(2L, "tv"))).willReturn(Optional.empty());
        given(tmdbImageClient.download("/abc123.jpg")).willReturn(new byte[] {1, 2, 3});
        given(fileStorageService.storeBytes(any(), any())).willReturn("/uploads/local.jpg");
        given(contentRepository.save(any(Content.class))).willAnswer(inv -> inv.getArgument(0));

        Content result = contentService.findOrCreate(2L, "tv", "/abc123.jpg");

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        then(contentRepository).should().save(captor.capture());
        Content saved = captor.getValue();
        assertThat(saved).isSameAs(result);
        assertThat(saved.getPosterPath()).isEqualTo("/abc123.jpg");
        assertThat(saved.getThumbnailPath()).isEqualTo("/uploads/local.jpg");
        then(fileStorageService).should().storeBytes(new byte[] {1, 2, 3}, "jpg");
    }

    @Test
    void findOrCreate_다운로드_실패해도_posterPath는_저장된다() {
        given(contentRepository.findById(ContentId.of(3L, "movie"))).willReturn(Optional.empty());
        given(tmdbImageClient.download("/fail.jpg")).willReturn(null);
        given(contentRepository.save(any(Content.class))).willAnswer(inv -> inv.getArgument(0));

        Content result = contentService.findOrCreate(3L, "movie", "/fail.jpg");

        assertThat(result.getPosterPath()).isEqualTo("/fail.jpg");
        assertThat(result.getThumbnailPath()).isNull();
        then(fileStorageService).shouldHaveNoInteractions();
    }

    @Test
    void findOrCreate_posterPath가_비어있으면_둘_다_null이다() {
        given(contentRepository.findById(ContentId.of(4L, "movie"))).willReturn(Optional.empty());
        given(contentRepository.save(any(Content.class))).willAnswer(inv -> inv.getArgument(0));

        Content result = contentService.findOrCreate(4L, "movie", "  ");

        assertThat(result.getPosterPath()).isNull();
        assertThat(result.getThumbnailPath()).isNull();
        then(tmdbImageClient).shouldHaveNoInteractions();
    }
}
