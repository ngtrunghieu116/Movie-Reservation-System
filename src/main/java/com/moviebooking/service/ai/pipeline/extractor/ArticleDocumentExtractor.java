package com.moviebooking.service.ai.pipeline.extractor;

import com.moviebooking.dto.ai.KnowledgeDocument;
import com.moviebooking.model.Article;
import com.moviebooking.model.enums.ArticleStatus;
import com.moviebooking.repository.ArticleRepository;
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
 * Trích xuất bài viết đã xuất bản (PUBLISHED) và chuyển thành KnowledgeDocument.
 * TUYỆT ĐỐI không index bài viết có status DRAFT hoặc HIDDEN.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleDocumentExtractor {

    private final ArticleRepository articleRepository;
    private final DocumentTransformer transformer;

    /**
     * Trích xuất tất cả bài viết đã PUBLISHED theo batch phân trang.
     */
    public List<KnowledgeDocument> extractAll(int batchSize) {
        List<KnowledgeDocument> documents = new ArrayList<>();
        int page = 0;

        Page<Article> articlePage;
        do {
            articlePage = articleRepository.findByStatus(ArticleStatus.PUBLISHED, PageRequest.of(page, batchSize));
            for (Article article : articlePage.getContent()) {
                try {
                    documents.add(transformer.transformArticle(article));
                } catch (Exception e) {
                    log.error("Failed to transform article id={}: {}", article.getId(), e.getMessage());
                }
            }
            page++;
        } while (articlePage.hasNext());

        log.info("Extracted {} published article documents (DRAFT/HIDDEN excluded)", documents.size());
        return documents;
    }

    /**
     * Trích xuất một bài viết theo ID (chỉ nếu PUBLISHED).
     */
    public Optional<KnowledgeDocument> extractById(Long articleId) {
        return articleRepository.findById(articleId)
                .filter(a -> a.getStatus() == ArticleStatus.PUBLISHED)
                .map(transformer::transformArticle);
    }

    /**
     * Kiểm tra bài viết có cần xóa vector không.
     */
    public boolean shouldRemoveFromIndex(Long articleId) {
        return articleRepository.findById(articleId)
                .map(a -> a.getStatus() != ArticleStatus.PUBLISHED)
                .orElse(true);
    }
}
