package com.moviebooking.service.ai.pipeline.extractor;

import com.moviebooking.dto.ai.KnowledgeDocument;
import com.moviebooking.model.Review;
import com.moviebooking.model.enums.ReviewStatus;
import com.moviebooking.repository.ReviewRepository;
import com.moviebooking.service.ai.pipeline.transformer.DocumentTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Trích xuất review đã duyệt (PUBLISHED) và chuyển thành KnowledgeDocument.
 * TUYỆT ĐỐI không index review có status HIDDEN hoặc DELETED.
 * TUYỆT ĐỐI không đưa PII người dùng vào nội dung vector.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewDocumentExtractor {

    private final ReviewRepository reviewRepository;
    private final DocumentTransformer transformer;

    /**
     * Trích xuất tất cả review đã PUBLISHED theo batch phân trang.
     */
    public List<KnowledgeDocument> extractAll(int batchSize) {
        List<KnowledgeDocument> documents = new ArrayList<>();
        int page = 0;

        Page<Review> reviewPage;
        do {
            reviewPage = reviewRepository.findByStatus(ReviewStatus.PUBLISHED, PageRequest.of(page, batchSize));
            for (Review review : reviewPage.getContent()) {
                try {
                    documents.add(transformer.transformReview(review));
                } catch (Exception e) {
                    log.error("Failed to transform review id={}: {}", review.getId(), e.getMessage());
                }
            }
            page++;
        } while (reviewPage.hasNext());

        log.info("Extracted {} published review documents (HIDDEN/DELETED excluded)", documents.size());
        return documents;
    }

    /**
     * Trích xuất một review theo ID (chỉ nếu PUBLISHED).
     */
    public Optional<KnowledgeDocument> extractById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .filter(r -> r.getStatus() == ReviewStatus.PUBLISHED)
                .map(transformer::transformReview);
    }

    /**
     * Kiểm tra review có đang ở trạng thái cần xóa vector không.
     */
    public boolean shouldRemoveFromIndex(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(r -> r.getStatus() != ReviewStatus.PUBLISHED)
                .orElse(true); // Nếu không tìm thấy, cũng cần xóa
    }
}
