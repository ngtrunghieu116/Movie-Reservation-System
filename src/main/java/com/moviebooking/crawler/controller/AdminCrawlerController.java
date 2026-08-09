package com.moviebooking.crawler.controller;

import com.moviebooking.crawler.dto.CrawlerSummaryResponse;
import com.moviebooking.crawler.orchestrator.CrawlerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoint to manually trigger the movie crawler.
 * This controller provides a simple POST endpoint for admin to
 * initiate crawling of movies from the NCC website.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/crawler")
@RequiredArgsConstructor
public class AdminCrawlerController {

    private final CrawlerManager crawlerManager;

    /**
     * Trigger a full crawl of movies from the configured source.
     * POST /api/admin/crawler/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<CrawlerSummaryResponse> triggerCrawl() {
        log.info("[INFO] Admin triggered manual crawl");
        CrawlerSummaryResponse summary = crawlerManager.crawlAll();
        return ResponseEntity.ok(summary);
    }
}
