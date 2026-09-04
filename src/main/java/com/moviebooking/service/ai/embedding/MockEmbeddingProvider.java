package com.moviebooking.service.ai.embedding;

import com.moviebooking.config.AiPipelineProperties;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock embedding provider cho test, CI/CD và development offline.
 * Tạo vector tất định (deterministic) dựa trên SHA-256 hash của nội dung,
 * đảm bảo cùng input luôn cho cùng output mà không cần internet hoặc API key.
 */
@Slf4j
public class MockEmbeddingProvider implements EmbeddingProvider {

    private final int dimension;

    public MockEmbeddingProvider(AiPipelineProperties properties) {
        this.dimension = properties.getEmbedding().getDimension();
        log.info("MockEmbeddingProvider initialized with dimension={}", dimension);
    }

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(generateDeterministicVector(text));
        }
        return results;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    /**
     * Tạo pseudo-embedding tất định từ SHA-256 hash.
     * Vector được chuẩn hóa (unit-normalized) để mô phỏng đúng hành vi
     * của embedding models thực tế.
     */
    private float[] generateDeterministicVector(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            float[] vector = new float[dimension];
            for (int i = 0; i < dimension; i++) {
                // Sử dụng byte hash lặp lại để lấp đầy vector
                int byteIdx = i % hash.length;
                vector[i] = ((hash[byteIdx] & 0xFF) - 128) / 128.0f;
            }

            // Chuẩn hóa L2 norm
            float norm = 0f;
            for (float v : vector) {
                norm += v * v;
            }
            norm = (float) Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < vector.length; i++) {
                    vector[i] /= norm;
                }
            }

            return vector;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
