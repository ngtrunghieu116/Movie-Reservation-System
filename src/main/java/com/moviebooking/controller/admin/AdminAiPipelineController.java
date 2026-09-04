package com.moviebooking.controller.admin;

import com.moviebooking.dto.ai.ReindexRequest;
import com.moviebooking.dto.ai.ReindexSummaryResponse;
import com.moviebooking.service.ai.pipeline.AiDataPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin API cho AI Data Pipeline.
 * Tất cả endpoints yêu cầu ROLE_ADMIN (được bảo vệ bởi SecurityConfig: /api/admin/**).
 *
 * Không expose bất kỳ endpoint công khai nào cho pipeline.
 */
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
@Slf4j
public class AdminAiPipelineController {

    private final AiDataPipelineService pipelineService;

    /**
     * Reindex toàn bộ hoặc theo lựa chọn.
     * POST /api/admin/ai/reindex
     */
    @PostMapping("/reindex")
    public ResponseEntity<ReindexSummaryResponse> fullReindex(
            @RequestBody(required = false) ReindexRequest request) {

        if (request == null) {
            request = new ReindexRequest(); // Mặc định reindex tất cả
        }

        log.info("Admin triggered full reindex: movies={}, reviews={}, articles={}, clear={}",
                request.isReindexMovies(), request.isReindexReviews(),
                request.isReindexArticles(), request.isClearExisting());

        ReindexSummaryResponse summary = pipelineService.fullReindex(request);
        return ResponseEntity.ok(summary);
    }

    /**
     * Reindex một phim cụ thể.
     * POST /api/admin/ai/reindex/movie/{id}
     */
    @PostMapping("/reindex/movie/{id}")
    public ResponseEntity<ReindexSummaryResponse> reindexMovie(@PathVariable Long id) {
        log.info("Admin triggered movie reindex: id={}", id);
        return ResponseEntity.ok(pipelineService.reindexMovie(id));
    }

    /**
     * Reindex một review cụ thể.
     * POST /api/admin/ai/reindex/review/{id}
     */
    @PostMapping("/reindex/review/{id}")
    public ResponseEntity<ReindexSummaryResponse> reindexReview(@PathVariable Long id) {
        log.info("Admin triggered review reindex: id={}", id);
        return ResponseEntity.ok(pipelineService.reindexReview(id));
    }

    /**
     * Reindex một bài viết cụ thể.
     * POST /api/admin/ai/reindex/article/{id}
     */
    @PostMapping("/reindex/article/{id}")
    public ResponseEntity<ReindexSummaryResponse> reindexArticle(@PathVariable Long id) {
        log.info("Admin triggered article reindex: id={}", id);
        return ResponseEntity.ok(pipelineService.reindexArticle(id));
    }

    /**
     * Lấy trạng thái hiện tại của AI pipeline và vector store.
     * GET /api/admin/ai/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(pipelineService.getStatus());
    }
}
