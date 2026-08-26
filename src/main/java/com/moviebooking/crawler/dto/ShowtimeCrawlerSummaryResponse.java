package com.moviebooking.crawler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Summary response for Showtime Crawler execution.
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeCrawlerSummaryResponse {
    private int totalFetched;
    private int inserted;
    private int updated;
    private int skipped;
    private int softDeactivated;
    private int failed;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private long executionTimeMs;
}
