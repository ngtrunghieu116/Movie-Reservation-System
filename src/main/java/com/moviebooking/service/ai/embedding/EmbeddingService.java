package com.moviebooking.service.ai.embedding;

import com.moviebooking.dto.ai.KnowledgeChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service chịu trách nhiệm gán embedding vectors cho KnowledgeChunks.
 * Ủy quyền việc tạo vector cho EmbeddingProvider hiện tại.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingProvider embeddingProvider;

    /**
     * Tạo embedding cho một đoạn text.
     */
    public float[] generateEmbedding(String text) {
        List<float[]> result = embeddingProvider.embed(List.of(text));
        return result.getFirst();
    }

    /**
     * Gán embedding vectors cho danh sách chunks.
     * Xử lý theo batch để tối ưu API calls.
     */
    public void embedChunks(List<KnowledgeChunk> chunks) {
        if (chunks.isEmpty()) return;

        List<String> texts = chunks.stream()
                .map(KnowledgeChunk::getText)
                .collect(Collectors.toList());

        log.debug("Generating embeddings for {} chunks using provider: {}",
                texts.size(), embeddingProvider.getProviderName());

        List<float[]> embeddings = embeddingProvider.embed(texts);

        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setEmbedding(embeddings.get(i));
        }

        log.debug("Embeddings generated successfully for {} chunks", chunks.size());
    }

    /**
     * Trả về số chiều vector của provider hiện tại.
     */
    public int getDimension() {
        return embeddingProvider.getDimension();
    }

    /**
     * Trả về tên provider hiện tại.
     */
    public String getProviderName() {
        return embeddingProvider.getProviderName();
    }
}
