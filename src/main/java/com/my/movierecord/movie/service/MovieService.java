package com.my.movierecord.movie.service;

import com.my.movierecord.movie.domain.Movie;
import com.my.movierecord.movie.repository.MovieRepository;
import com.my.movierecord.common.service.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final FileStorageService fileStorageService;

    public MovieService(MovieRepository movieRepository, FileStorageService fileStorageService) {
        this.movieRepository = movieRepository;
        this.fileStorageService = fileStorageService;
    }

    public Page<Movie> list(Pageable pageable) {
        return movieRepository.findAll(pageable);
    }

    public Movie get(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("영화를 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public Movie create(MovieSaveCommand command) {
        Movie movie = Movie.builder()
                .title(command.title())
                .watchedDate(command.watchedDate())
                .thumbnailPath(command.thumbnailPath())
                .oneLiner(command.oneLiner())
                .immersion(command.immersion())
                .story(command.story())
                .emotion(command.emotion())
                .goodPoints(command.goodPoints())
                .badPoints(command.badPoints())
                .taste(command.taste())
                .rating(command.rating())
                .build();
        return movieRepository.save(movie);
    }

    @Transactional
    public Movie update(Long id, MovieSaveCommand command, boolean replaceThumbnail) {
        Movie movie = get(id);
        String previousThumbnail = movie.getThumbnailPath();
        String nextThumbnail = replaceThumbnail ? command.thumbnailPath() : previousThumbnail;

        movie.update(
                command.title(),
                command.watchedDate(),
                nextThumbnail,
                command.oneLiner(),
                command.immersion(),
                command.story(),
                command.emotion(),
                command.goodPoints(),
                command.badPoints(),
                command.taste(),
                command.rating()
        );

        if (replaceThumbnail && previousThumbnail != null && !previousThumbnail.equals(nextThumbnail)) {
            fileStorageService.delete(previousThumbnail);
        }
        return movie;
    }

    @Transactional
    public void delete(Long id) {
        Movie movie = get(id);
        String thumbnail = movie.getThumbnailPath();
        movieRepository.delete(movie);
        if (thumbnail != null) {
            fileStorageService.delete(thumbnail);
        }
    }
}
