package com.my.movierecord.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path rootLocation;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalName);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("허용되지 않는 이미지 형식입니다. (jpg, jpeg, png, webp, gif 만 가능)");
        }
        try {
            Files.createDirectories(rootLocation);
            String newFilename = UUID.randomUUID() + "." + extension.toLowerCase();
            Path target = rootLocation.resolve(newFilename).normalize();
            if (!target.startsWith(rootLocation)) {
                throw new IllegalArgumentException("잘못된 업로드 경로입니다.");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return newFilename;
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 실패: " + e.getMessage(), e);
        }
    }

    public void delete(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        try {
            Path target = rootLocation.resolve(filename).normalize();
            if (!target.startsWith(rootLocation)) {
                return;
            }
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }
}
