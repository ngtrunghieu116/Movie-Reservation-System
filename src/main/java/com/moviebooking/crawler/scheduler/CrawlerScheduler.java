package com.moviebooking.crawler.scheduler;

import com.moviebooking.crawler.orchestrator.CrawlerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlerScheduler {

    private final CrawlerManager crawlerManager;

    // Run every day at 02:00 AM
    @Scheduled(cron = "${crawler.schedule.cron:0 0 2 * * ?}")
    public void scheduleDailyCrawler() {
        log.info("[INFO] Starting scheduled crawler job...");
        try {
            crawlerManager.crawlAll();
            log.info("[INFO] Scheduled crawler job finished successfully.");
        } catch (Exception e) {
            log.error("[ERROR] Scheduled crawler job failed", e);
        }
    }
}
