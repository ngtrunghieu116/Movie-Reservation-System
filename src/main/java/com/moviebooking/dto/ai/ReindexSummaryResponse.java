package com.moviebooking.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả thực thi pipeline reindex.
 * Cung cấp thông tin quan sát (observability) cho Admin.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReindexSummaryResponse {

    public enum Status {
        COMPLETED,
        COMPLETED_WITH_ERRORS,
        FAILED
    }

    private Status status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;

    @Builder.Default
    private SourceStats movies = new SourceStats();

    @Builder.Default
    private SourceStats reviews = new SourceStats();

    @Builder.Default
    private SourceStats articles = new SourceStats();

    private int totalChunksCreated;
    private int totalVectorsIndexed;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceStats {
        @Builder.Default
        private int extracted = 0;
        @Builder.Default
        private int indexed = 0;
        @Builder.Default
        private int skipped = 0;
        @Builder.Default
        private int failed = 0;
    }
}
