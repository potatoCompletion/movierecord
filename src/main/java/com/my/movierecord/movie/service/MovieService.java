package com.my.movierecord.movie.service;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.common.service.FileStorageService;
import com.my.movierecord.movie.domain.Movie;
import com.my.movierecord.movie.repository.MovieRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 영화 기록 비즈니스 로직을 담당하는 서비스.
 * 영화의 조회, 생성, 수정, 삭제 기능을 제공한다.
 * 썸네일 파일 관리도 함께 처리한다.
 */
@Service
@Transactional(readOnly = true)  // 읽기 전용 트랜잭션이 기본값 (명시적인 @Transactional 메서드는 write 트랜잭션)
public class MovieService {

    private final MovieRepository movieRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public MovieService(MovieRepository movieRepository, FileStorageService fileStorageService,
                        UserRepository userRepository) {
        this.movieRepository = movieRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    /**
     * 페이지 단위로 영화 목록을 조회한다.
     * Pageable에 포함된 정렬 설정(latest, rating, title)이 적용된다.
     */
    public Page<Movie> list(Pageable pageable) {
        return movieRepository.findAll(pageable);
    }

    /**
     * ID로 단일 영화를 조회한다.
     * 존재하지 않으면 EntityNotFoundException을 던진다.
     */
    public Movie get(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("영화를 찾을 수 없습니다. id=" + id));
    }

    /**
     * 새 영화 기록을 생성한다.
     * MovieSaveCommand의 모든 정보를 이용하여 Movie 엔티티를 빌더로 구성한다.
     */
    @Transactional
    public Movie create(MovieSaveCommand command) {
        User user = userRepository.getReferenceById(command.userId());
        Movie movie = Movie.builder()
                .title(command.title())
                .watchedDate(command.watchedDate())
                .thumbnailPath(command.thumbnailPath())
                .oneLiner(command.oneLiner())
                .immersion(command.immersion())
                .story(command.story())
                .emotions(command.emotions())
                .goodPoints(command.goodPoints())
                .badPoints(command.badPoints())
                .taste(command.taste())
                .rating(command.rating())
                .user(user)
                .build();
        return movieRepository.save(movie);
    }

    public Page<Movie> listByUser(Long userId, Pageable pageable) {
        return movieRepository.findByUserId(userId, pageable);
    }

    /**
     * 기존 영화 기록을 수정한다.
     * replaceThumbnail이 true일 때만 새로운 썸네일로 교체하고, 이전 썸네일을 삭제한다.
     * replaceThumbnail이 false이면 기존 썸네일 경로를 유지한다.
     *
     * @param id 수정할 영화 ID
     * @param command 새로운 영화 정보
     * @param replaceThumbnail 썸네일 파일 교체 여부
     */
    @Transactional
    public Movie update(Long id, MovieSaveCommand command, boolean replaceThumbnail) {
        Movie movie = get(id);  // ID로 기존 영화 조회 (없으면 예외 발생)
        String previousThumbnail = movie.getThumbnailPath();
        // replaceThumbnail이 true면 새 파일명 사용, false면 기존 파일명 유지
        String nextThumbnail = replaceThumbnail ? command.thumbnailPath() : previousThumbnail;

        // 엔티티의 update() 메서드로 모든 필드 수정
        movie.update(
                command.title(),
                command.watchedDate(),
                nextThumbnail,
                command.oneLiner(),
                command.immersion(),
                command.story(),
                command.emotions(),
                command.goodPoints(),
                command.badPoints(),
                command.taste(),
                command.rating()
        );

        // 썸네일이 교체되었으면 이전 파일 삭제 (저장소 공간 절약)
        if (replaceThumbnail && previousThumbnail != null && !previousThumbnail.equals(nextThumbnail)) {
            fileStorageService.delete(previousThumbnail);
        }
        return movie;
    }

    /**
     * 영화 기록을 삭제한다.
     * 관련된 썸네일 파일도 함께 삭제된다.
     */
    @Transactional
    public void delete(Long id) {
        Movie movie = get(id);  // ID로 영화 조회
        String thumbnail = movie.getThumbnailPath();
        movieRepository.delete(movie);  // DB에서 영화 기록 삭제
        // 썸네일 파일이 있으면 파일 시스템에서도 삭제
        if (thumbnail != null) {
            fileStorageService.delete(thumbnail);
        }
    }
}
