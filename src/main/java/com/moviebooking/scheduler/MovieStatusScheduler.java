package com.moviebooking.scheduler;

import com.moviebooking.service.movie.MovieStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieStatusScheduler {

    private final MovieStatusService movieStatusService;

    /**
     * Scheduled job running daily at 00:01 AM to update movie statuses.
     * Keeps movie lifecycle in sync with current date.
     */
    @Scheduled(cron = "${movie.status.schedule.cron:0 1 0 * * ?}")
    public void scheduleMovieStatusUpdate() {
        log.info("[INFO] Starting scheduled movie status update job...");
        try {
            int updatedCount = movieStatusService.updateAllStatuses();
            log.info("[INFO] Scheduled movie status update job completed. Updated {} movies.", updatedCount);
        } catch (Exception e) {
            log.error("[ERROR] Scheduled movie status update job failed", e);
        }
    }
}
