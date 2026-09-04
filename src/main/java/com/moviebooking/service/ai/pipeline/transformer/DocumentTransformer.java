package com.moviebooking.service.ai.pipeline.transformer;

import com.moviebooking.dto.ai.KnowledgeDocument;
import com.moviebooking.dto.ai.SourceType;
import com.moviebooking.model.Article;
import com.moviebooking.model.Genre;
import com.moviebooking.model.Movie;
import com.moviebooking.model.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chuyển đổi entity nguồn thành KnowledgeDocument chuẩn hóa.
 * Bảo đảm:
 * - Phân biệt rõ Fact vs. Opinion (Review)
 * - Loại bỏ 100% User PII
 * - Làm sạch HTML (Article)
 * - Bảo toàn ngữ nghĩa và nhận dạng nguồn
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentTransformer {

    private final TextCleaner textCleaner;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== MOVIE ====================

    public KnowledgeDocument transformMovie(Movie movie) {
        String genreNames = movie.getGenres() != null
                ? movie.getGenres().stream()
                    .map(Genre::getName)
                    .sorted()
                    .collect(Collectors.joining(", "))
                : "";

        StringBuilder sb = new StringBuilder();
        sb.append("[MOVIE KNOWLEDGE]\n");
        sb.append("Title: ").append(nullSafe(movie.getTitle())).append("\n");
        if (movie.getTitleEn() != null && !movie.getTitleEn().isBlank()) {
            sb.append("English Title: ").append(movie.getTitleEn()).append("\n");
        }
        if (!genreNames.isEmpty()) {
            sb.append("Genres: ").append(genreNames).append("\n");
        }
        sb.append("Director: ").append(nullSafe(movie.getDirector())).append("\n");
        sb.append("Actors: ").append(nullSafe(movie.getActors())).append("\n");
        sb.append("Duration: ").append(movie.getDuration()).append(" minutes\n");
        sb.append("Age Rating: ").append(movie.getAgeRating() != null ? movie.getAgeRating().name() : "").append("\n");
        sb.append("Language: ").append(nullSafe(movie.getLanguage())).append("\n");
        if (movie.getSubtitle() != null && !movie.getSubtitle().isBlank()) {
            sb.append("Subtitle: ").append(movie.getSubtitle()).append("\n");
        }
        if (movie.getReleaseDate() != null) {
            sb.append("Release Date: ").append(movie.getReleaseDate().format(DATE_FMT)).append("\n");
        }
        sb.append("Status: ").append(movie.getStatus() != null ? movie.getStatus().name() : "").append("\n");
        sb.append("\nSynopsis:\n").append(textCleaner.normalizeWhitespace(nullSafe(movie.getDescription())));

        String content = sb.toString();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("status", movie.getStatus() != null ? movie.getStatus().name() : null);
        metadata.put("ageRating", movie.getAgeRating() != null ? movie.getAgeRating().name() : null);
        metadata.put("genres", genreNames);
        metadata.put("director", movie.getDirector());
        metadata.put("duration", movie.getDuration());
        if (movie.getReleaseDate() != null) {
            metadata.put("releaseDate", movie.getReleaseDate().toString());
        }

        return KnowledgeDocument.builder()
                .documentId("doc:movie:" + movie.getId())
                .sourceType(SourceType.MOVIE)
                .sourceId(movie.getId())
                .movieId(movie.getId())
                .title(movie.getTitle())
                .content(content)
                .metadata(metadata)
                .contentHash(sha256(content))
                .sourceCreatedAt(null)  // Movie entity không có createdAt
                .sourceUpdatedAt(null)
                .build();
    }

    // ==================== REVIEW ====================

    public KnowledgeDocument transformReview(Review review) {
        // CRITICAL: Không bao giờ đưa User PII vào nội dung vector
        String movieTitle = review.getMovie() != null ? review.getMovie().getTitle() : "Unknown";
        Long movieId = review.getMovie() != null ? review.getMovie().getId() : null;
        boolean verified = Boolean.TRUE.equals(review.getVerifiedPurchase());

        StringBuilder sb = new StringBuilder();
        sb.append("[AUDIENCE REVIEW]\n");
        sb.append("Movie: ").append(movieTitle).append("\n");
        sb.append("Rating: ").append(review.getRating()).append("/5 stars\n");
        sb.append("Verified Ticket Buyer: ").append(verified ? "Yes" : "No").append("\n");
        if (review.getCreatedAt() != null) {
            sb.append("Date: ").append(review.getCreatedAt().toLocalDate().format(DATE_FMT)).append("\n");
        }
        sb.append("\nReview Comment:\n").append(textCleaner.normalizeWhitespace(nullSafe(review.getComment())));

        String content = sb.toString();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("rating", review.getRating());
        metadata.put("verifiedPurchase", verified);
        metadata.put("movieTitle", movieTitle);
        if (review.getCreatedAt() != null) {
            metadata.put("createdAt", review.getCreatedAt().toString());
        }

        return KnowledgeDocument.builder()
                .documentId("doc:review:" + review.getId())
                .sourceType(SourceType.REVIEW)
                .sourceId(review.getId())
                .movieId(movieId)
                .title("Review: " + movieTitle)
                .content(content)
                .metadata(metadata)
                .contentHash(sha256(content))
                .sourceCreatedAt(review.getCreatedAt())
                .sourceUpdatedAt(review.getUpdatedAt())
                .build();
    }

    // ==================== ARTICLE ====================

    public KnowledgeDocument transformArticle(Article article) {
        // Làm sạch HTML trong content
        String cleanContent = textCleaner.cleanHtml(article.getContent());
        String cleanShortDesc = textCleaner.normalizeWhitespace(nullSafe(article.getShortDescription()));

        // Nội dung đầy đủ (trước khi chia chunk) — kèm Context Header
        StringBuilder sb = new StringBuilder();
        sb.append("[ARTICLE: ").append(nullSafe(article.getTitle())).append("]\n");
        sb.append("Summary: ").append(cleanShortDesc).append("\n\n");
        sb.append("Content:\n").append(cleanContent);

        String content = sb.toString();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("status", article.getStatus() != null ? article.getStatus().name() : null);
        if (article.getCreatedAt() != null) {
            metadata.put("createdAt", article.getCreatedAt().toString());
        }
        if (article.getUpdatedAt() != null) {
            metadata.put("updatedAt", article.getUpdatedAt().toString());
        }

        return KnowledgeDocument.builder()
                .documentId("doc:article:" + article.getId())
                .sourceType(SourceType.ARTICLE)
                .sourceId(article.getId())
                .movieId(null)  // Article không liên kết trực tiếp với Movie
                .title(article.getTitle())
                .content(content)
                .metadata(metadata)
                .contentHash(sha256(content))
                .sourceCreatedAt(article.getCreatedAt())
                .sourceUpdatedAt(article.getUpdatedAt())
                .build();
    }

    // ==================== UTILITIES ====================

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    /**
     * Tạo SHA-256 hash cho nội dung để phát hiện thay đổi.
     */
    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có trong JDK, trường hợp này không bao giờ xảy ra
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
