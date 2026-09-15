package com.my.movierecord.movie.repository;

import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.movie.domain.ContentId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContentRepository extends JpaRepository<Content, ContentId> {

    /**
     * poster_path 백필 대상: TMDB 상세 API 로 채울 수 있는데 아직 비어 있는 콘텐츠.
     * 백필 완료 후 임시 엔드포인트와 함께 제거한다.
     */
    @Query("select c from Content c where c.posterPath is null and c.id.tmdbId is not null")
    List<Content> findAllMissingPosterPath();
}
