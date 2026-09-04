package com.moviebooking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình trung tâm cho AI Data Pipeline.
 * Tất cả tham số pipeline đều có giá trị mặc định an toàn
 * để chạy được trong môi trường dev/test mà không cần cấu hình bên ngoài.
 */
@Component
@ConfigurationProperties(prefix = "ai")
@Getter
@Setter
public class AiPipelineProperties {

    private Embedding embedding = new Embedding();
    private VectorStoreConfig vectorstore = new VectorStoreConfig();
    private Pipeline pipeline = new Pipeline();

    @Getter
    @Setter
    public static class Embedding {
        /** mock | gemini | openai */
        private String provider = "mock";
        private String apiKey = "mock-key";
        private String model = "text-embedding-004";
        private int dimension = 768;
        private String baseUrl = "https://generativelanguage.googleapis.com";
    }

    @Getter
    @Setter
    public static class VectorStoreConfig {
        /** memory | qdrant */
        private String provider = "memory";
        private String host = "localhost";
        private int port = 6334;
        private String collectionName = "cinemind_knowledge";
    }

    @Getter
    @Setter
    public static class Pipeline {
        /** Số lượng entity mỗi trang khi trích xuất batch */
        private int batchSize = 50;
        /** Kích thước tối đa mỗi chunk (ký tự) */
        private int maxChunkSize = 1500;
        /** Số ký tự overlap giữa các chunk */
        private int chunkOverlap = 200;
    }
}
