package com.moviebooking.service.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.poster-dir:uploads/posters/}")
    private String posterDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    public String storePosterFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File ảnh poster không được để trống!");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Kích thước file vượt quá giới hạn tối đa 5MB!");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Định dạng file không hợp lệ! Chỉ chấp nhận ảnh (JPEG, PNG, WEBP).");
        }

        try {
            Path uploadPath = Paths.get(posterDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                fileExtension = ".jpg";
            }

            String newFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(newFilename);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/posters/" + newFilename;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu trữ file poster: " + e.getMessage());
        }
    }
}
