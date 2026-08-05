package com.survivaldiary.domain.user.service;

import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ProfileImageStorage {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final String PUBLIC_PATH = "/uploads/profile/";
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final Path storageDirectory;

    public ProfileImageStorage(
            @Value("${app.upload.profile-directory:uploads/profile}")
            String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile image) {
        validate(image);
        String extension = EXTENSIONS.get(image.getContentType());
        String filename = UUID.randomUUID() + extension;
        Path target = storageDirectory.resolve(filename).normalize();
        if (!target.startsWith(storageDirectory)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일 이름이 올바르지 않습니다.");
        }

        try {
            Files.createDirectories(storageDirectory);
            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return PUBLIC_PATH + filename;
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "프로필 사진을 저장하지 못했습니다."
            );
        }
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(PUBLIC_PATH)) {
            return;
        }
        String filename = imageUrl.substring(PUBLIC_PATH.length());
        Path target = storageDirectory.resolve(filename).normalize();
        if (!target.startsWith(storageDirectory)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "이전 프로필 사진을 정리하지 못했습니다."
            );
        }
    }

    private void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "프로필 사진을 선택해 주세요.");
        }
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "프로필 사진은 5MB 이하여야 합니다.");
        }
        if (!EXTENSIONS.containsKey(image.getContentType())) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "JPG, PNG, WEBP, GIF 형식의 이미지만 등록할 수 있습니다."
            );
        }
    }
}
