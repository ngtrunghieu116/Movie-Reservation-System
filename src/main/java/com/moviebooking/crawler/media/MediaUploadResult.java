package com.moviebooking.crawler.media;

public record MediaUploadResult(
        String url,
        boolean success,
        String originalUrl
) {
}
