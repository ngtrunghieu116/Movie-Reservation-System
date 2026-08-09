package com.moviebooking.crawler.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryMediaStorage implements MediaStorage {

    private final Cloudinary cloudinary;

    @Override
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public MediaUploadResult uploadFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            log.warn("Image URL is empty. Skipping upload.");
            return new MediaUploadResult(null, false, imageUrl);
        }
        
        try {
            log.info("Starting to upload image to Cloudinary from URL: {}", imageUrl);
            // Uploading from URL is supported natively by Cloudinary
            Map<?, ?> uploadResult = cloudinary.uploader().upload(imageUrl, ObjectUtils.asMap(
                    "folder", "cinemind/posters",
                    "use_filename", true,
                    "unique_filename", true
            ));
            
            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Successfully uploaded image to Cloudinary: {}", secureUrl);
            return new MediaUploadResult(secureUrl, true, imageUrl);
            
        } catch (Exception e) {
            log.error("Failed to upload image to Cloudinary from URL: {}. Reason: {}", imageUrl, e.getMessage(), e);
            // Return failure result instead of throwing exception to allow the crawler to proceed
            return new MediaUploadResult(null, false, imageUrl);
        }
    }
}
