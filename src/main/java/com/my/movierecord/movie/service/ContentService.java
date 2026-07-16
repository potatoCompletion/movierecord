package com.my.movierecord.movie.service;

import com.my.movierecord.common.service.FileStorageService;
import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.movie.domain.ContentId;
import com.my.movierecord.movie.repository.ContentRepository;
import com.my.movierecord.tmdb.client.TmdbImageClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;
    private final FileStorageService fileStorageService;
    private final TmdbImageClient tmdbImageClient;

    public ContentService(ContentRepository contentRepository,
            FileStorageService fileStorageService,
            TmdbImageClient tmdbImageClient) {
        this.contentRepository = contentRepository;
        this.fileStorageService = fileStorageService;
        this.tmdbImageClient = tmdbImageClient;
    }

    @Transactional
    public Content findOrCreate(Long tmdbId, String mediaType, String posterPath) {
        return contentRepository.findById(ContentId.of(tmdbId, mediaType))
                .orElseGet(() -> {
                    Content content = Content.of(tmdbId, mediaType);
                    if (posterPath != null && !posterPath.isBlank()) {
                        String localPath = downloadAndSave(posterPath);
                        content.updateThumbnailPath(localPath);
                    }
                    return contentRepository.save(content);
                });
    }

    private String downloadAndSave(String posterPath) {
        byte[] bytes = tmdbImageClient.download(posterPath);
        if (bytes == null) {
            return null;
        }
        String extension = posterPath.contains(".")
                ? posterPath.substring(posterPath.lastIndexOf('.') + 1).toLowerCase()
                : "jpg";
        return fileStorageService.storeBytes(bytes, extension);
    }
}
