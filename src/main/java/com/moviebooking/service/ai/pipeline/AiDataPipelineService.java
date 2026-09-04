package com.moviebooking.service.ai.pipeline;

import com.moviebooking.config.AiPipelineProperties;
import com.moviebooking.dto.ai.*;
import com.moviebooking.service.ai.embedding.EmbeddingService;
import com.moviebooking.service.ai.pipeline.chunker.DocumentChunker;
import com.moviebooking.service.ai.pipeline.extractor.ArticleDocumentExtractor;
import com.moviebooking.service.ai.pipeline.extractor.MovieDocumentExtractor;
import com.moviebooking.service.ai.pipeline.extractor.ReviewDocumentExtractor;
import com.moviebooking.service.ai.pipeline.validator.DocumentValidator;
import com.moviebooking.service.ai.vectorstore.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dịch vụ điều phối AI Data Pipeline.
 * Quản lý toàn bộ luồng: Extract → Transform → Validate → Chunk → Embed → Index.
 *
 * Nguyên tắc:
 * - Lỗi 1 tài liệu KHÔNG làm dừng toàn bộ batch
 * - Idempotent: chạy nhiều lần cho cùng kết quả
 * - Chỉ ĐỌC dữ liệu business, chỉ GHI vào vector store
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiDataPipelineService {

    private final MovieDocumentExtractor movieExtractor;
    private final ReviewDocumentExtractor reviewExtractor;
    private final ArticleDocumentExtractor articleExtractor;
    private final DocumentValidator validator;
    private final DocumentChunker chunker;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final AiPipelineProperties properties;

    // ==================== FULL REINDEX ====================

    /**
     * Thực hiện reindex toàn bộ (hoặc theo lựa chọn) nguồn dữ liệu AI.
     */
    public ReindexSummaryResponse fullReindex(ReindexRequest request) {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("=== AI DATA PIPELINE: Full Reindex STARTED ===");

        ReindexSummaryResponse summary = ReindexSummaryResponse.builder()
                .startTime(startTime)
                .build();

        int totalChunks = 0;
        int totalVectors = 0;

        try {
            // Khởi tạo collection nếu chưa tồn tại
            vectorStore.initializeCollection();

            // Xóa toàn bộ nếu được yêu cầu
            if (request.isClearExisting()) {
                log.info("Clearing existing vectors...");
                vectorStore.clearCollection();
                vectorStore.initializeCollection();
            }

            // 1. Movies
            if (request.isReindexMovies()) {
                ProcessingResult movieResult = processSource(SourceType.MOVIE);
                summary.setMovies(movieResult.stats);
                totalChunks += movieResult.chunksCreated;
                totalVectors += movieResult.vectorsIndexed;
                summary.getErrors().addAll(movieResult.errors);
            }

            // 2. Reviews
            if (request.isReindexReviews()) {
                ProcessingResult reviewResult = processSource(SourceType.REVIEW);
                summary.setReviews(reviewResult.stats);
                totalChunks += reviewResult.chunksCreated;
                totalVectors += reviewResult.vectorsIndexed;
                summary.getErrors().addAll(reviewResult.errors);
            }

            // 3. Articles
            if (request.isReindexArticles()) {
                ProcessingResult articleResult = processSource(SourceType.ARTICLE);
                summary.setArticles(articleResult.stats);
                totalChunks += articleResult.chunksCreated;
                totalVectors += articleResult.vectorsIndexed;
                summary.getErrors().addAll(articleResult.errors);
            }

            summary.setTotalChunksCreated(totalChunks);
            summary.setTotalVectorsIndexed(totalVectors);
            summary.setStatus(summary.getErrors().isEmpty()
                    ? ReindexSummaryResponse.Status.COMPLETED
                    : ReindexSummaryResponse.Status.COMPLETED_WITH_ERRORS);

        } catch (Exception e) {
            log.error("Pipeline infrastructure failure: {}", e.getMessage(), e);
            summary.setStatus(ReindexSummaryResponse.Status.FAILED);
            summary.getErrors().add("Infrastructure failure: " + e.getMessage());
        }

        LocalDateTime endTime = LocalDateTime.now();
        summary.setEndTime(endTime);
        summary.setDurationMs(Duration.between(startTime, endTime).toMillis());

        log.info("=== AI DATA PIPELINE: Full Reindex {} in {}ms — chunks={}, vectors={}, errors={} ===",
                summary.getStatus(), summary.getDurationMs(),
                totalChunks, totalVectors, summary.getErrors().size());

        return summary;
    }

    // ==================== SINGLE ENTITY REINDEX ====================

    /**
     * Reindex một phim cụ thể.
     */
    public ReindexSummaryResponse reindexMovie(Long movieId) {
        return reindexSingleEntity(SourceType.MOVIE, movieId,
                () -> movieExtractor.extractById(movieId));
    }

    /**
     * Reindex một review cụ thể.
     * Nếu review không còn PUBLISHED, xóa vector tương ứng.
     */
    public ReindexSummaryResponse reindexReview(Long reviewId) {
        if (reviewExtractor.shouldRemoveFromIndex(reviewId)) {
            return removeFromIndex(SourceType.REVIEW, reviewId);
        }
        return reindexSingleEntity(SourceType.REVIEW, reviewId,
                () -> reviewExtractor.extractById(reviewId));
    }

    /**
     * Reindex một bài viết cụ thể.
     * Nếu article không còn PUBLISHED, xóa vector tương ứng.
     */
    public ReindexSummaryResponse reindexArticle(Long articleId) {
        if (articleExtractor.shouldRemoveFromIndex(articleId)) {
            return removeFromIndex(SourceType.ARTICLE, articleId);
        }
        return reindexSingleEntity(SourceType.ARTICLE, articleId,
                () -> articleExtractor.extractById(articleId));
    }

    // ==================== STATUS ====================

    /**
     * Lấy trạng thái hiện tại của vector store.
     */
    public java.util.Map<String, Object> getStatus() {
        java.util.Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("available", vectorStore.isAvailable());
        status.put("totalVectors", vectorStore.count());
        status.put("bySourceType", vectorStore.countBySourceType());
        status.put("embeddingProvider", embeddingService.getProviderName());
        status.put("embeddingDimension", embeddingService.getDimension());
        return status;
    }

    // ==================== INTERNAL ====================

    /**
     * Xử lý toàn bộ một loại nguồn dữ liệu.
     */
    private ProcessingResult processSource(SourceType sourceType) {
        int batchSize = properties.getPipeline().getBatchSize();
        ProcessingResult result = new ProcessingResult();

        log.info("Processing source: {}", sourceType);

        // 1. Extract
        List<KnowledgeDocument> documents = switch (sourceType) {
            case MOVIE -> movieExtractor.extractAll(batchSize);
            case REVIEW -> reviewExtractor.extractAll(batchSize);
            case ARTICLE -> articleExtractor.extractAll(batchSize);
        };

        result.stats.setExtracted(documents.size());

        // 2. Process each document through Validate → Chunk → Embed → Index
        for (KnowledgeDocument doc : documents) {
            try {
                // Validate
                if (!validator.isValid(doc)) {
                    result.stats.setSkipped(result.stats.getSkipped() + 1);
                    continue;
                }

                // Xóa vectors cũ của source này trước khi upsert mới (idempotency)
                vectorStore.deleteBySource(doc.getSourceType(), doc.getSourceId());

                // Chunk
                List<KnowledgeChunk> chunks = chunker.chunk(doc);
                result.chunksCreated += chunks.size();

                // Embed
                embeddingService.embedChunks(chunks);

                // Index
                vectorStore.upsert(chunks);
                result.vectorsIndexed += chunks.size();
                result.stats.setIndexed(result.stats.getIndexed() + 1);

            } catch (Exception e) {
                log.error("Failed to process document {}: {}", doc.getDocumentId(), e.getMessage());
                result.stats.setFailed(result.stats.getFailed() + 1);
                result.errors.add(doc.getDocumentId() + ": " + e.getMessage());
            }
        }

        log.info("{}: extracted={}, indexed={}, skipped={}, failed={}",
                sourceType, result.stats.getExtracted(), result.stats.getIndexed(),
                result.stats.getSkipped(), result.stats.getFailed());

        return result;
    }

    /**
     * Reindex một entity đơn lẻ.
     */
    private ReindexSummaryResponse reindexSingleEntity(
            SourceType sourceType, Long entityId,
            java.util.function.Supplier<Optional<KnowledgeDocument>> extractor) {

        LocalDateTime startTime = LocalDateTime.now();
        ReindexSummaryResponse summary = ReindexSummaryResponse.builder().startTime(startTime).build();
        ReindexSummaryResponse.SourceStats stats = new ReindexSummaryResponse.SourceStats();

        try {
            vectorStore.initializeCollection();

            Optional<KnowledgeDocument> docOpt = extractor.get();
            if (docOpt.isEmpty()) {
                stats.setSkipped(1);
                summary.setStatus(ReindexSummaryResponse.Status.COMPLETED);
            } else {
                KnowledgeDocument doc = docOpt.get();
                stats.setExtracted(1);

                if (!validator.isValid(doc)) {
                    stats.setSkipped(1);
                } else {
                    vectorStore.deleteBySource(sourceType, entityId);
                    List<KnowledgeChunk> chunks = chunker.chunk(doc);
                    embeddingService.embedChunks(chunks);
                    vectorStore.upsert(chunks);
                    stats.setIndexed(1);
                    summary.setTotalChunksCreated(chunks.size());
                    summary.setTotalVectorsIndexed(chunks.size());
                }
                summary.setStatus(ReindexSummaryResponse.Status.COMPLETED);
            }
        } catch (Exception e) {
            log.error("Failed to reindex {} id={}: {}", sourceType, entityId, e.getMessage());
            stats.setFailed(1);
            summary.setStatus(ReindexSummaryResponse.Status.FAILED);
            summary.getErrors().add(e.getMessage());
        }

        switch (sourceType) {
            case MOVIE -> summary.setMovies(stats);
            case REVIEW -> summary.setReviews(stats);
            case ARTICLE -> summary.setArticles(stats);
        }

        LocalDateTime endTime = LocalDateTime.now();
        summary.setEndTime(endTime);
        summary.setDurationMs(Duration.between(startTime, endTime).toMillis());
        return summary;
    }

    /**
     * Xóa vectors của entity không còn đủ điều kiện index.
     */
    private ReindexSummaryResponse removeFromIndex(SourceType sourceType, Long entityId) {
        LocalDateTime startTime = LocalDateTime.now();
        vectorStore.deleteBySource(sourceType, entityId);
        log.info("Removed vectors for {}:{} from index", sourceType, entityId);

        LocalDateTime endTime = LocalDateTime.now();
        return ReindexSummaryResponse.builder()
                .status(ReindexSummaryResponse.Status.COMPLETED)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(Duration.between(startTime, endTime).toMillis())
                .build();
    }

    /**
     * Internal result holder cho processing.
     */
    private static class ProcessingResult {
        ReindexSummaryResponse.SourceStats stats = new ReindexSummaryResponse.SourceStats();
        int chunksCreated = 0;
        int vectorsIndexed = 0;
        List<String> errors = new ArrayList<>();
    }
}
