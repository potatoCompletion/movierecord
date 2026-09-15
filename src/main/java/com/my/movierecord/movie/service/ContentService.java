package com.my.movierecord.movie.service;

import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.movie.domain.ContentId;
import com.my.movierecord.movie.repository.ContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;

    public ContentService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    /**
     * (tmdbId, mediaType) 에 해당하는 콘텐츠를 찾고, 없으면 posterPath 만 저장해 새로 만든다.
     * 포스터는 TMDB CDN 에서 직접 서빙하므로 이미지를 내려받지 않는다.
     */
    @Transactional
    public Content findOrCreate(Long tmdbId, String mediaType, String posterPath) {
        return contentRepository.findById(ContentId.of(tmdbId, mediaType))
                .orElseGet(() -> {
                    Content content = Content.of(tmdbId, mediaType);
                    if (posterPath != null && !posterPath.isBlank()) {
                        content.updatePosterPath(posterPath);
                    }
                    return contentRepository.save(content);
                });
    }
}
