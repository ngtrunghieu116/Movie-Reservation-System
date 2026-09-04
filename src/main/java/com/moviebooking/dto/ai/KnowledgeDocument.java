package com.moviebooking.dto.ai;

import lombok.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Mô hình tài liệu tri thức AI nội bộ (không persist vào MySQL).
 * Đại diện cho dữ liệu đã được trích xuất và chuẩn hóa từ nguồn gốc,
 * sẵn sàng để chia chunk và vector hóa.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeDocument {

    /** ID tất định: "doc:movie:1", "doc:review:42", "doc:article:10" */
    private String documentId;

    private SourceType sourceType;

    /** Primary Key của entity nguồn trong MySQL */
    private Long sourceId;

    /** ID phim liên kết (null cho Article độc lập) */
    private Long movieId;

    /** Tiêu đề tài liệu hoặc tham chiếu */
    private String title;

    /** Nội dung văn bản đã chuẩn hóa */
    private String content;

    /** Metadata cấu trúc (status, rating, verifiedPurchase, v.v.) */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /** SHA-256 hash nội dung để phát hiện thay đổi */
    private String contentHash;

    private LocalDateTime sourceCreatedAt;
    private LocalDateTime sourceUpdatedAt;
}
