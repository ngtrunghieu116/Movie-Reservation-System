package com.moviebooking.service.ai.vectorstore;

import com.moviebooking.dto.ai.KnowledgeChunk;
import com.moviebooking.dto.ai.SourceType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory vector store implementation cho test, CI/CD và development.
 * Không yêu cầu Docker hay bất kỳ infrastructure bên ngoài.
 *
 * Lưu trữ vectors trong ConcurrentHashMap với khả năng:
 * - Upsert idempotent (ghi đè theo chunkId)
 * - Delete theo source
 * - Count theo sourceType
 */
@Slf4j
public class InMemoryVectorStore implements VectorStore {

    private final ConcurrentHashMap<String, StoredVector> vectors = new ConcurrentHashMap<>();

    /**
     * Internal representation của vector đã lưu.
     */
    private record StoredVector(
            String chunkId,
            float[] embedding,
            SourceType sourceType,
            Long sourceId,
            Long movieId,
            Map<String, Object> metadata
    ) {}

    @Override
    public void initializeCollection() {
        log.info("InMemoryVectorStore: collection initialized (in-memory)");
    }

    @Override
    public void upsert(List<KnowledgeChunk> chunks) {
        for (KnowledgeChunk chunk : chunks) {
            if (chunk.getEmbedding() == null) {
                log.warn("Skipping chunk {} — no embedding", chunk.getChunkId());
                continue;
            }

            Map<String, Object> payload = new HashMap<>(chunk.getMetadata());
            payload.put("sourceType", chunk.getSourceType().name());
            payload.put("sourceId", chunk.getSourceId());
            if (chunk.getMovieId() != null) {
                payload.put("movieId", chunk.getMovieId());
            }
            payload.put("chunkIndex", chunk.getChunkIndex());
            payload.put("totalChunks", chunk.getTotalChunks());

            vectors.put(chunk.getChunkId(), new StoredVector(
                    chunk.getChunkId(),
                    chunk.getEmbedding().clone(),
                    chunk.getSourceType(),
                    chunk.getSourceId(),
                    chunk.getMovieId(),
                    payload
            ));
        }
        log.debug("InMemoryVectorStore: upserted {} vectors (total: {})", chunks.size(), vectors.size());
    }

    @Override
    public void deleteBySource(SourceType sourceType, Long sourceId) {
        List<String> toRemove = vectors.entrySet().stream()
                .filter(e -> e.getValue().sourceType() == sourceType
                        && Objects.equals(e.getValue().sourceId(), sourceId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        toRemove.forEach(vectors::remove);
        log.debug("InMemoryVectorStore: deleted {} vectors for {}:{}", toRemove.size(), sourceType, sourceId);
    }

    @Override
    public void deleteById(String vectorId) {
        vectors.remove(vectorId);
    }

    @Override
    public void clearCollection() {
        int size = vectors.size();
        vectors.clear();
        log.info("InMemoryVectorStore: cleared {} vectors", size);
    }

    @Override
    public long count() {
        return vectors.size();
    }

    @Override
    public Map<String, Long> countBySourceType() {
        return vectors.values().stream()
                .collect(Collectors.groupingBy(
                        v -> v.sourceType().name(),
                        Collectors.counting()
                ));
    }

    @Override
    public boolean isAvailable() {
        return true; // In-memory luôn sẵn sàng
    }

    // ==================== TEST HELPERS ====================

    /**
     * Kiểm tra vector tồn tại theo ID (chỉ dùng cho tests).
     */
    public boolean containsVector(String chunkId) {
        return vectors.containsKey(chunkId);
    }

    /**
     * Lấy metadata của vector (chỉ dùng cho tests).
     */
    public Map<String, Object> getVectorMetadata(String chunkId) {
        StoredVector v = vectors.get(chunkId);
        return v != null ? v.metadata() : null;
    }
}
