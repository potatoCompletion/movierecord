package com.my.movierecord.movie.service;

import com.my.movierecord.common.service.FileStorageService;
import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.movie.domain.ContentId;
import com.my.movierecord.movie.repository.ContentRepository;
import com.my.movierecord.tmdb.config.TmdbProperties;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;
    private final FileStorageService fileStorageService;
    private final TmdbProperties tmdbProperties;

    public ContentService(ContentRepository contentRepository,
                          FileStorageService fileStorageService,
                          TmdbProperties tmdbProperties) {
        this.contentRepository = contentRepository;
        this.fileStorageService = fileStorageService;
        this.tmdbProperties = tmdbProperties;
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
        String imageUrl = tmdbProperties.imageBaseUrl() + posterPath;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.connect();
            try (InputStream is = conn.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                String extension = posterPath.contains(".")
                        ? posterPath.substring(posterPath.lastIndexOf('.') + 1).toLowerCase()
                        : "jpg";
                return fileStorageService.storeBytes(bytes, extension);
            }
        } catch (IOException e) {
            return null;
        }
    }
}
