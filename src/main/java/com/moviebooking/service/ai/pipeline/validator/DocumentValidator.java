package com.moviebooking.service.ai.pipeline.validator;

import com.moviebooking.dto.ai.KnowledgeDocument;
import com.moviebooking.dto.ai.SourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Xác thực KnowledgeDocument trước khi chuyển sang giai đoạn Chunk & Embed.
 * Tài liệu không hợp lệ sẽ bị bỏ qua (skip) thay vì gây lỗi toàn bộ batch.
 */
@Component
@Slf4j
public class DocumentValidator {

    /**
     * Kiểm tra tính hợp lệ của KnowledgeDocument.
     *
     * @return true nếu hợp lệ để tiếp tục pipeline, false nếu cần skip
     */
    public boolean isValid(KnowledgeDocument doc) {
        if (doc == null) {
            log.warn("Validation failed: document is null");
            return false;
        }

        if (doc.getDocumentId() == null || doc.getDocumentId().isBlank()) {
            log.warn("Validation failed: documentId is blank");
            return false;
        }

        if (doc.getSourceType() == null) {
            log.warn("Validation failed: sourceType is null for document {}", doc.getDocumentId());
            return false;
        }

        if (doc.getSourceId() == null) {
            log.warn("Validation failed: sourceId is null for document {}", doc.getDocumentId());
            return false;
        }

        if (doc.getContent() == null || doc.getContent().isBlank()) {
            log.warn("Validation failed: content is blank for document {}", doc.getDocumentId());
            return false;
        }

        // Kiểm tra tên tiêu đề cho Movie và Article
        if (doc.getSourceType() != SourceType.REVIEW) {
            if (doc.getTitle() == null || doc.getTitle().isBlank()) {
                log.warn("Validation failed: title is blank for {} document {}",
                        doc.getSourceType(), doc.getDocumentId());
                return false;
            }
        }

        // Kiểm tra rating hợp lệ cho Review
        if (doc.getSourceType() == SourceType.REVIEW) {
            Object rating = doc.getMetadata().get("rating");
            if (rating instanceof Integer r) {
                if (r < 1 || r > 5) {
                    log.warn("Validation failed: rating {} is out of range [1-5] for document {}",
                            r, doc.getDocumentId());
                    return false;
                }
            } else {
                log.warn("Validation failed: rating is missing or invalid type for review document {}",
                        doc.getDocumentId());
                return false;
            }
        }

        return true;
    }
}
