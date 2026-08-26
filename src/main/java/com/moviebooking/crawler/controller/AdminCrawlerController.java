package com.moviebooking.crawler.controller;

import com.moviebooking.crawler.dto.CrawlerSummaryResponse;
import com.moviebooking.crawler.dto.ShowtimeCrawlerSummaryResponse;
import com.moviebooking.crawler.orchestrator.CrawlerManager;
import com.moviebooking.crawler.orchestrator.ShowtimeCrawlerOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoints to manually trigger Movie and Showtime crawlers.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/crawler")
@RequiredArgsConstructor
public class AdminCrawlerController {

    private final CrawlerManager crawlerManager;
    private final ShowtimeCrawlerOrchestrator showtimeCrawlerOrchestrator;

    /**
     * Trigger a full crawl of movies from the configured source.
     * POST /api/admin/crawler/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<CrawlerSummaryResponse> triggerCrawl() {
        log.info("[INFO] Admin triggered manual movie crawl");
        CrawlerSummaryResponse summary = crawlerManager.crawlAll();
        return ResponseEntity.ok(summary);
    }

    /**
     * Trigger a full crawl and sync of showtimes from the configured source.
     * POST /api/admin/crawler/showtimes/trigger
     */
    @PostMapping("/showtimes/trigger")
    public ResponseEntity<ShowtimeCrawlerSummaryResponse> triggerShowtimeCrawl(
            @RequestParam(name = "theaterId", required = false, defaultValue = "1") Long theaterId) {
        log.info("[INFO] Admin triggered manual showtime crawl for theaterId={}", theaterId);
        ShowtimeCrawlerSummaryResponse summary = showtimeCrawlerOrchestrator.crawlShowtimes(theaterId);
        return ResponseEntity.ok(summary);
    }
}
