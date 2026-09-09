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

    @Value("${app.upload.banner-dir:uploads/banners/}")
    private String bannerDir;

    @Value("${app.upload.product-dir:uploads/products/}")
    private String productDir;

    @Value("${app.upload.article-dir:uploads/articles/}")
    private String articleDir;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif",
            "image/avif", "image/svg+xml", "image/bmp", "image/x-icon", "image/vnd.microsoft.icon",
            "image/tiff", "image/heic", "image/heif", "image/jfif", "image/pjpeg"
    );

    private boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String contentType = file.getContentType();
        if (contentType != null) {
            String lower = contentType.toLowerCase().trim();
            if (lower.startsWith("image/") || ALLOWED_CONTENT_TYPES.contains(lower)) {
                return true;
            }
        }
        String filename = file.getOriginalFilename();
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
            return ext.matches("\\.(jpe?g|png|webp|gif|avif|svg|bmp|ico|tiff?|heic|heif|jfif)$");
        }
        return false;
    }

    private String extractExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            switch (contentType.toLowerCase().trim()) {
                case "image/png": return ".png";
                case "image/webp": return ".webp";
                case "image/avif": return ".avif";
                case "image/gif": return ".gif";
                case "image/svg+xml": return ".svg";
                case "image/bmp": return ".bmp";
                case "image/x-icon":
                case "image/vnd.microsoft.icon": return ".ico";
                case "image/heic": return ".heic";
                case "image/heif": return ".heif";
                case "image/tiff": return ".tiff";
                default: return ".jpg";
            }
        }
        return ".jpg";
    }

    public String storePosterFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File ảnh poster không được để trống!");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Kích thước file vượt quá giới hạn tối đa 10MB!");
        }

        if (!isValidImage(file)) {
            throw new RuntimeException("Định dạng file không hợp lệ! Vui lòng chọn file hình ảnh (JPEG, PNG, WEBP, AVIF, GIF, SVG, BMP...).");
        }

        try {
            Path uploadPath = Paths.get(posterDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileExtension = extractExtension(file);
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

    public String storeBannerFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Kích thước file banner vượt quá giới hạn tối đa 10MB!");
        }

        if (!isValidImage(file)) {
            throw new RuntimeException("Định dạng file banner không hợp lệ! Vui lòng chọn file hình ảnh (JPEG, PNG, WEBP, AVIF, GIF, SVG, BMP...).");
        }

        try {
            Path uploadPath = Paths.get(bannerDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileExtension = extractExtension(file);
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(newFilename);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/banners/" + newFilename;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu trữ file banner: " + e.getMessage());
        }
    }

    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh sản phẩm không được để trống!");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Kích thước file vượt quá giới hạn tối đa 10MB!");
        }

        if (!isValidImage(file)) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ! Vui lòng chọn file hình ảnh.");
        }

        try {
            Path uploadPath = Paths.get(productDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileExtension = extractExtension(file);
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(newFilename);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/products/" + newFilename;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu trữ file ảnh sản phẩm: " + e.getMessage());
        }
    }

    public String storeArticlePoster(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh bài viết không được để trống!");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Kích thước file vượt quá giới hạn tối đa 10MB!");
        }

        if (!isValidImage(file)) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ! Vui lòng chọn file hình ảnh.");
        }

        try {
            Path uploadPath = Paths.get(articleDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileExtension = extractExtension(file);
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(newFilename);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/articles/" + newFilename;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu trữ file ảnh bài viết: " + e.getMessage());
        }
    }

    public void deleteFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return;
        }
        
        // filePath format is like /uploads/products/filename.jpg
        // We need to remove the leading / if present
        String relativePath = filePath;
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        try {
            Path path = Paths.get(relativePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + filePath + ", error: " + e.getMessage());
        }
    }
}
