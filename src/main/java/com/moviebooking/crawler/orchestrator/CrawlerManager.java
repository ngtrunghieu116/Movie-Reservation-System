package com.moviebooking.crawler.orchestrator;

import com.moviebooking.crawler.client.CrawlerClient;
import com.moviebooking.crawler.dto.CrawlerSummaryResponse;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerManager {

    private final CrawlerClient crawlerClient;
    private final CrawlerOrchestrator orchestrator;

    public CrawlerSummaryResponse crawlAll() {
        log.info("[INFO] Start crawl Source={}", crawlerClient.getName());
        LocalDateTime startTime = LocalDateTime.now();
        
        List<MovieListItemDTO> movies = crawlerClient.fetchMovieList();
        
        int totalFetched = movies.size();
        int inserted = 0;
        int skipped = 0;
        int failed = 0;

        for (MovieListItemDTO item : movies) {
            try {
                boolean success = orchestrator.processMovie(item);
                if (success) {
                    inserted++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                // Logging is already handled in orchestrator
                failed++;
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        long executionTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
        
        CrawlerSummaryResponse summary = CrawlerSummaryResponse.builder()
                .totalFetched(totalFetched)
                .inserted(inserted)
                .skipped(skipped)
                .failed(failed)
                .startedAt(startTime)
                .finishedAt(endTime)
                .executionTimeMs(executionTimeMs)
                .build();
                
        log.info("[INFO] Finish crawl Source={}. Summary: inserted={}, skipped={}, failed={}, time={}ms", 
                crawlerClient.getName(), inserted, skipped, failed, executionTimeMs);
                
        return summary;
    }
}
