package com.moviebooking.service.ai.vectorstore;

import com.moviebooking.dto.ai.KnowledgeChunk;
import com.moviebooking.dto.ai.SourceType;

import java.util.List;
import java.util.Map;

/**
 * Abstraction cho vector store.
 * Phase 2 sử dụng: upsert, delete, initialize, count.
 * Search được chuẩn bị sẵn cho Phase 3 Hybrid Retrieval.
 */
public interface VectorStore {

    /** Khởi tạo collection nếu chưa tồn tại */
    void initializeCollection();

    /** Upsert (insert hoặc overwrite) danh sách chunks đã có embedding */
    void upsert(List<KnowledgeChunk> chunks);

    /** Xóa tất cả vectors thuộc một nguồn cụ thể (sourceType + sourceId) */
    void deleteBySource(SourceType sourceType, Long sourceId);

    /** Xóa một vector theo ID */
    void deleteById(String vectorId);

    /** Xóa toàn bộ collection */
    void clearCollection();

    /** Đếm tổng số vectors trong collection */
    long count();

    /** Đếm số vectors theo sourceType */
    Map<String, Long> countBySourceType();

    /** Kiểm tra vector store có sẵn sàng không */
    boolean isAvailable();
}
