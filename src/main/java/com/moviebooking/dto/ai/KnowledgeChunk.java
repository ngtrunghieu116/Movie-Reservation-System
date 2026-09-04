package com.moviebooking.dto.ai;

import lombok.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Đơn vị tri thức nhỏ nhất để vector hóa.
 * Mỗi KnowledgeChunk chứa đủ ngữ cảnh để tự giải thích được
 * khi được truy xuất từ Vector Store.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeChunk {

    /** ID vector tất định: "movie_1", "review_42", "article_10_c0" */
    private String chunkId;

    /** Tham chiếu đến KnowledgeDocument cha */
    private String documentId;

    private SourceType sourceType;

    /** Primary Key entity nguồn */
    private Long sourceId;

    /** ID phim liên kết */
    private Long movieId;

    /** Vị trí chunk (0-indexed) */
    private int chunkIndex;

    /** Tổng số chunk trong document này */
    private int totalChunks;

    /** Nội dung văn bản có thể embedding (kèm Context Header) */
    private String text;

    /** Metadata payload lưu vào Vector Store cho filtering */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /** Dense vector sau khi qua Embed Stage */
    private float[] embedding;
}
