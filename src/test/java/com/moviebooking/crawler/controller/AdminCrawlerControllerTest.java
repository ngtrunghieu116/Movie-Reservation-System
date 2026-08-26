package com.moviebooking.crawler.controller;

import com.moviebooking.crawler.dto.CrawlerSummaryResponse;
import com.moviebooking.crawler.dto.ShowtimeCrawlerSummaryResponse;
import com.moviebooking.crawler.orchestrator.CrawlerManager;
import com.moviebooking.crawler.orchestrator.ShowtimeCrawlerOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCrawlerControllerTest {

    @Mock
    private CrawlerManager crawlerManager;

    @Mock
    private ShowtimeCrawlerOrchestrator showtimeCrawlerOrchestrator;

    @InjectMocks
    private AdminCrawlerController controller;

    @Test
    void triggerCrawl_ShouldReturnMovieSummary() {
        CrawlerSummaryResponse mockSummary = CrawlerSummaryResponse.builder()
                .totalFetched(10)
                .inserted(8)
                .skipped(2)
                .failed(0)
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .executionTimeMs(500)
                .build();

        when(crawlerManager.crawlAll()).thenReturn(mockSummary);

        ResponseEntity<CrawlerSummaryResponse> response = controller.triggerCrawl();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().getTotalFetched());
        assertEquals(8, response.getBody().getInserted());
        verify(crawlerManager, times(1)).crawlAll();
    }

    @Test
    void triggerShowtimeCrawl_ShouldReturnShowtimeSummary() {
        ShowtimeCrawlerSummaryResponse mockSummary = ShowtimeCrawlerSummaryResponse.builder()
                .totalFetched(50)
                .inserted(40)
                .updated(5)
                .skipped(3)
                .softDeactivated(2)
                .failed(0)
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .executionTimeMs(1200)
                .build();

        when(showtimeCrawlerOrchestrator.crawlShowtimes(1L)).thenReturn(mockSummary);

        ResponseEntity<ShowtimeCrawlerSummaryResponse> response = controller.triggerShowtimeCrawl(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(50, response.getBody().getTotalFetched());
        assertEquals(40, response.getBody().getInserted());
        assertEquals(5, response.getBody().getUpdated());
        assertEquals(3, response.getBody().getSkipped());
        assertEquals(2, response.getBody().getSoftDeactivated());
        verify(showtimeCrawlerOrchestrator, times(1)).crawlShowtimes(1L);
    }
}
