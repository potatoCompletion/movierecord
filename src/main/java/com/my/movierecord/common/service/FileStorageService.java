package com.my.movierecord.common.service;

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

/**
 * 업로드된 파일을 관리하는 서비스.
 * 이미지 파일의 저장 및 삭제를 처리하며, 경로 조작 공격(path traversal)으로부터 보호한다.
 * 허용된 확장자: jpg, jpeg, png, webp, gif
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path rootLocation;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 업로드된 파일을 저장한다.
     * 파일이 null이거나 비어있으면 null을 반환한다.
     * 허용되지 않는 확장자이거나 경로 조작 시도가 있으면 예외를 throw한다.
     *
     * @param file 저장할 파일
     * @return 저장된 파일명 (UUID + 확장자), 파일이 null/empty인 경우 null
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalName);

        // 확장자 검증 (허용된 이미지 형식만 가능)
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("허용되지 않는 이미지 형식입니다. (jpg, jpeg, png, webp, gif 만 가능)");
        }

        try {
            Files.createDirectories(rootLocation);
            // UUID를 파일명으로 사용하여 파일명 충돌 방지
            String newFilename = UUID.randomUUID() + "." + extension.toLowerCase();
            Path target = rootLocation.resolve(newFilename).normalize();

            // 경로 조작 공격 방지: 저장 경로가 루트 위치 내에 있는지 확인
            if (!target.startsWith(rootLocation)) {
                throw new IllegalArgumentException("잘못된 업로드 경로입니다.");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return newFilename;
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 파일을 삭제한다.
     * 파일명이 null이거나 비어있으면 아무것도 하지 않는다.
     * 경로 조작 시도가 있으면 삭제하지 않고 반환한다.
     * IOException은 무시되어 조용히 실패한다.
     *
     * @param filename 삭제할 파일명
     */
    public void delete(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        try {
            Path target = rootLocation.resolve(filename).normalize();
            // 경로 조작 공격 방지
            if (!target.startsWith(rootLocation)) {
                return;
            }
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // IOException 무시 - 이미 삭제된 파일이거나 권한 문제 등은 무시
        }
    }
}
