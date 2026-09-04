package com.moviebooking.config;

import com.moviebooking.service.ai.embedding.EmbeddingProvider;
import com.moviebooking.service.ai.embedding.MockEmbeddingProvider;
import com.moviebooking.service.ai.vectorstore.InMemoryVectorStore;
import com.moviebooking.service.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Cấu hình Bean cho AI Pipeline.
 * Mặc định sử dụng Mock Embedding + InMemory Vector Store
 * để đảm bảo ứng dụng chạy được mà không cần infrastructure bên ngoài.
 *
 * Cấu hình production:
 *   ai.embedding.provider=gemini
 *   ai.vectorstore.provider=qdrant
 */
@Configuration
public class AiPipelineConfig {

    /**
     * Mock embedding provider — mặc định khi không có cấu hình.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "ai.embedding.provider", havingValue = "mock", matchIfMissing = true)
    public EmbeddingProvider mockEmbeddingProvider(AiPipelineProperties properties) {
        return new MockEmbeddingProvider(properties);
    }

    /**
     * In-memory vector store — mặc định khi không có cấu hình.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "ai.vectorstore.provider", havingValue = "memory", matchIfMissing = true)
    public VectorStore inMemoryVectorStore() {
        return new InMemoryVectorStore();
    }
}
