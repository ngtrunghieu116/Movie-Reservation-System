package com.moviebooking.crawler.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CrawlerSummaryResponse {
    private int totalFetched;
    private int inserted;
    private int skipped;
    private int failed;
    
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private long executionTimeMs;
}
