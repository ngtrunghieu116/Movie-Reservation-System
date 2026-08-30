package com.moviebooking.scheduler;

import com.moviebooking.service.booking.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupScheduler {

    private final BookingService bookingService;

    /**
     * Periodically cleans up expired pending reservations.
     * Runs every 60 seconds.
     */
    @Scheduled(fixedDelay = 60000)
    public void cleanupExpiredReservationsJob() {
        try {
            int cleanedCount = bookingService.cleanupExpiredReservations();
            if (cleanedCount > 0) {
                log.info("[SCHEDULER_CLEANUP] Automatically expired {} pending reservations.", cleanedCount);
            }
        } catch (Exception e) {
            log.error("[SCHEDULER_ERROR] Failed to cleanup expired reservations: {}", e.getMessage(), e);
        }
    }
}
