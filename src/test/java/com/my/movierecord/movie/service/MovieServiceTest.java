package com.my.movierecord.movie.service;

import com.my.movierecord.common.service.FileStorageService;
import com.my.movierecord.movie.domain.Movie;
import com.my.movierecord.movie.repository.MovieRepository;
import com.my.movierecord.support.MovieFixture;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    MovieRepository movieRepository;

    @Mock
    FileStorageService fileStorageService;

    @InjectMocks
    MovieService movieService;

    @Test
    void list_repository_위임() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Movie> page = new PageImpl<>(List.of(MovieFixture.createMovieWithId(1L)), pageable, 1);
        given(movieRepository.findAll(pageable)).willReturn(page);

        Page<Movie> result = movieService.list(pageable);

        assertThat(result).isEqualTo(page);
        then(movieRepository).should().findAll(pageable);
    }

    @Test
    void get_존재하는_id_반환() {
        Movie movie = MovieFixture.createMovieWithId(1L);
        given(movieRepository.findById(1L)).willReturn(Optional.of(movie));

        Movie result = movieService.get(1L);

        assertThat(result.getTitle()).isEqualTo(movie.getTitle());
    }

    @Test
    void get_없는_id_EntityNotFoundException() {
        given(movieRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.get(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_save_호출_및_반환() {
        MovieSaveCommand command = MovieFixture.createCommand();
        Movie movie = MovieFixture.createMovieWithId(1L);
        given(movieRepository.save(any(Movie.class))).willReturn(movie);

        Movie result = movieService.create(command);

        then(movieRepository).should().save(any(Movie.class));
        assertThat(result).isEqualTo(movie);
    }

    @Test
    void update_replaceThumbnail_false_삭제_안됨() {
        Movie movie = MovieFixture.createMovieWithThumbnail(1L, "old.jpg");
        given(movieRepository.findById(1L)).willReturn(Optional.of(movie));

        movieService.update(1L, MovieFixture.createCommandWithThumbnail("new.jpg"), false);

        then(fileStorageService).should(never()).delete(any());
    }

    @Test
    void update_replaceThumbnail_true_기존_썸네일_없으면_삭제_안됨() {
        Movie movie = MovieFixture.createMovieWithId(1L);
        given(movieRepository.findById(1L)).willReturn(Optional.of(movie));

        movieService.update(1L, MovieFixture.createCommandWithThumbnail("new.jpg"), true);

        then(fileStorageService).should(never()).delete(any());
    }

    @Test
    void update_replaceThumbnail_true_기존_썸네일_있으면_삭제_호출() {
        Movie movie = MovieFixture.createMovieWithThumbnail(1L, "old.jpg");
        given(movieRepository.findById(1L)).willReturn(Optional.of(movie));

        movieService.update(1L, MovieFixture.createCommandWithThumbnail("new.jpg"), true);

        then(fileStorageService).should().delete("old.jpg");
    }

    @Test
    void update_replaceThumbnail_true_같은_파일명_삭제_안됨() {
        Movie movie = MovieFixture.createMovieWithThumbnail(1L, "same.jpg");
        given(movieRepository.findById(1L)).willReturn(Optional.of(movie));

        movieService.update(1L, MovieFixture.createCommandWithThumbnail("same.jpg"), true);

        then(fileStorageService).should(never()).delete(any());
    }

    @Test
    void delete_썸네일_있음_파일_삭제_호출() {
        Movie movie = MovieFixture.createMovieWithThumbnail(1L, "img.jpg");
        given(movieRepository.findById(1L)).willReturn(Optional.of(movie));

        movieService.delete(1L);

        then(fileStorageService).should().delete("img.jpg");
    }

    @Test
    void delete_썸네일_없음_삭제_안됨() {
        Movie movie = MovieFixture.createMovieWithId(1L);
        given(movieRepository.findById(1L)).willReturn(Optional.of(movie));

        movieService.delete(1L);

        then(fileStorageService).should(never()).delete(any());
    }

    @Test
    void delete_없는_id_예외_전파() {
        given(movieRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
