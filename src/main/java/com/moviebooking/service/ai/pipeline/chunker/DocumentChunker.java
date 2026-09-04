package com.moviebooking.service.ai.pipeline.chunker;

import com.moviebooking.config.AiPipelineProperties;
import com.moviebooking.dto.ai.KnowledgeChunk;
import com.moviebooking.dto.ai.KnowledgeDocument;
import com.moviebooking.dto.ai.SourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chia KnowledgeDocument thành các KnowledgeChunk có thể embedding.
 *
 * Chiến lược:
 * - Movie: 1 chunk duy nhất (metadata + synopsis là khối ngữ nghĩa thống nhất)
 * - Review: 1 chunk duy nhất (comment tối đa 1000 ký tự)
 * - Article: Sliding window (~1500 ký tự, overlap 200 ký tự)
 *            Mỗi chunk đều có Context Header (tiêu đề + tóm tắt)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentChunker {

    private final AiPipelineProperties properties;

    /**
     * Chia KnowledgeDocument thành 1 hoặc nhiều KnowledgeChunk.
     */
    public List<KnowledgeChunk> chunk(KnowledgeDocument doc) {
        return switch (doc.getSourceType()) {
            case MOVIE -> createSingleChunk(doc, "movie_" + doc.getSourceId());
            case REVIEW -> createSingleChunk(doc, "review_" + doc.getSourceId());
            case ARTICLE -> chunkArticle(doc);
        };
    }

    /**
     * Tạo 1 chunk duy nhất cho Movie hoặc Review.
     */
    private List<KnowledgeChunk> createSingleChunk(KnowledgeDocument doc, String chunkId) {
        KnowledgeChunk chunk = KnowledgeChunk.builder()
                .chunkId(chunkId)
                .documentId(doc.getDocumentId())
                .sourceType(doc.getSourceType())
                .sourceId(doc.getSourceId())
                .movieId(doc.getMovieId())
                .chunkIndex(0)
                .totalChunks(1)
                .text(doc.getContent())
                .metadata(new HashMap<>(doc.getMetadata()))
                .build();
        return List.of(chunk);
    }

    /**
     * Chia Article thành nhiều chunk bằng sliding window.
     * Mỗi chunk được gắn Context Header để bảo toàn ngữ cảnh khi truy xuất riêng lẻ.
     */
    private List<KnowledgeChunk> chunkArticle(KnowledgeDocument doc) {
        int maxChunkSize = properties.getPipeline().getMaxChunkSize();
        int overlap = properties.getPipeline().getChunkOverlap();
        String content = doc.getContent();

        // Nếu nội dung ngắn hơn maxChunkSize, giữ 1 chunk
        if (content.length() <= maxChunkSize) {
            return createSingleChunk(doc, "article_" + doc.getSourceId() + "_c0");
        }

        // Tạo Context Header cho mỗi chunk
        String contextHeader = buildArticleContextHeader(doc);
        int headerLength = contextHeader.length();
        int effectiveMaxSize = maxChunkSize - headerLength;

        if (effectiveMaxSize < 200) {
            // Header quá dài, giữ nguyên 1 chunk
            log.warn("Article {} has very long header, keeping as single chunk", doc.getSourceId());
            return createSingleChunk(doc, "article_" + doc.getSourceId() + "_c0");
        }

        // Tách phần Content body (bỏ header gốc đã có trong document)
        String bodyContent = extractBodyContent(content);

        List<String> textChunks = splitByBoundary(bodyContent, effectiveMaxSize, overlap);
        int totalChunks = textChunks.size();

        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            String chunkText = contextHeader + textChunks.get(i);
            String chunkId = "article_" + doc.getSourceId() + "_c" + i;

            Map<String, Object> chunkMeta = new HashMap<>(doc.getMetadata());
            chunkMeta.put("chunkIndex", i);
            chunkMeta.put("totalChunks", totalChunks);

            chunks.add(KnowledgeChunk.builder()
                    .chunkId(chunkId)
                    .documentId(doc.getDocumentId())
                    .sourceType(doc.getSourceType())
                    .sourceId(doc.getSourceId())
                    .movieId(doc.getMovieId())
                    .chunkIndex(i)
                    .totalChunks(totalChunks)
                    .text(chunkText)
                    .metadata(chunkMeta)
                    .build());
        }

        log.debug("Article {} chunked into {} chunks", doc.getSourceId(), totalChunks);
        return chunks;
    }

    /**
     * Tạo Context Header cho article chunk.
     */
    private String buildArticleContextHeader(KnowledgeDocument doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ARTICLE: ").append(doc.getTitle()).append("]\n");

        // Lấy shortDescription từ metadata hoặc nội dung
        String summary = extractSummaryFromContent(doc.getContent());
        if (!summary.isEmpty()) {
            sb.append("Summary: ").append(summary).append("\n\n");
        }
        sb.append("Content:\n");
        return sb.toString();
    }

    /**
     * Trích xuất phần Summary từ nội dung đã format.
     */
    private String extractSummaryFromContent(String content) {
        int summaryStart = content.indexOf("Summary: ");
        if (summaryStart < 0) return "";
        int summaryEnd = content.indexOf("\n\n", summaryStart);
        if (summaryEnd < 0) return "";
        return content.substring(summaryStart + "Summary: ".length(), summaryEnd).trim();
    }

    /**
     * Trích xuất phần body từ nội dung đã format (sau "Content:\n").
     */
    private String extractBodyContent(String content) {
        int contentStart = content.indexOf("Content:\n");
        if (contentStart < 0) return content;
        return content.substring(contentStart + "Content:\n".length());
    }

    /**
     * Chia văn bản theo ranh giới tự nhiên: đoạn văn > câu > khoảng trắng.
     */
    private List<String> splitByBoundary(String text, int maxSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());

            if (end < text.length()) {
                // Tìm ranh giới tự nhiên gần nhất trước end
                int boundary = findBestBoundary(text, start + (maxSize / 2), end);
                if (boundary > start) {
                    end = boundary;
                }
            }

            chunks.add(text.substring(start, end).trim());

            // Tiến tới vị trí tiếp theo có overlap
            start = Math.max(start + 1, end - overlap);

            // Tránh vòng lặp vô hạn
            if (start >= text.length()) break;
        }

        return chunks;
    }

    /**
     * Tìm ranh giới cắt tốt nhất trong khoảng [minPos, maxPos].
     * Ưu tiên: đoạn văn (\n\n) > câu (. ? !) > khoảng trắng > maxPos.
     */
    private int findBestBoundary(String text, int minPos, int maxPos) {
        // Ưu tiên 1: Ranh giới đoạn văn
        int paragraphBreak = text.lastIndexOf("\n\n", maxPos);
        if (paragraphBreak >= minPos) return paragraphBreak + 2;

        // Ưu tiên 2: Ranh giới câu
        for (String delimiter : new String[]{". ", "? ", "! "}) {
            int sentenceBreak = text.lastIndexOf(delimiter, maxPos);
            if (sentenceBreak >= minPos) return sentenceBreak + delimiter.length();
        }

        // Ưu tiên 3: Xuống dòng đơn
        int lineBreak = text.lastIndexOf("\n", maxPos);
        if (lineBreak >= minPos) return lineBreak + 1;

        // Ưu tiên 4: Khoảng trắng
        int spaceBreak = text.lastIndexOf(" ", maxPos);
        if (spaceBreak >= minPos) return spaceBreak + 1;

        // Fallback: cắt tại maxPos
        return maxPos;
    }
}
