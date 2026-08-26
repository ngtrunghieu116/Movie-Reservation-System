package com.moviebooking.crawler.orchestrator;

import com.moviebooking.crawler.client.CrawlerClient;
import com.moviebooking.crawler.dto.ShowtimeCrawlerSummaryResponse;
import com.moviebooking.crawler.dto.ShowtimeItemDTO;
import com.moviebooking.crawler.resolver.RoomResolver;
import com.moviebooking.crawler.resolver.ShowtimePriceParser;
import com.moviebooking.model.Movie;
import com.moviebooking.model.Room;
import com.moviebooking.model.Showtime;
import com.moviebooking.model.enums.MovieStatus;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ReservationRepository;
import com.moviebooking.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Showtime Crawler Orchestrator.
 *
 * Coordinates fetching, parsing, validating, resolving, idempotency checking,
 * persistence, and smart missing sync for NCC showtimes.
 *
 * Implements approved Business Rules:
 * - RULE-01: Never modify Movie.status or Movie.releaseDate.
 * - RULE-02: startTime < movie.releaseDate -> SKIP + LOG [INVALID_SHOWTIME_BEFORE_RELEASE_DATE].
 * - RULE-03: Movie matching via source_id. Movie COMING_SOON/ENDED -> SKIP + LOG [SKIP_MOVIE_STATUS].
 * - RULE-04/05/06: Smart Missing Sync (soft deactivate after 3 missing runs, protect showtimes with bookings).
 * - RULE-07/08: Room mapping via RoomResolver (UNMAPPED -> SKIP, MAINTENANCE -> SKIP).
 * - RULE-09: Idempotency via source_id ("ncc:" + sessionId).
 * - RULE-10: Price parsing with fallback to default policy (no genre pricing).
 * - RULE-11: Online selling status mapping.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeCrawlerOrchestrator {

    private final CrawlerClient crawlerClient;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ReservationRepository reservationRepository;
    private final RoomResolver roomResolver;
    private final ShowtimePriceParser priceParser;

    @Value("${app.showtime.buffer-time-minutes:15}")
    private int bufferTimeMinutes = 15;

    @Value("${crawler.default-price.standard:90000}")
    private BigDecimal defaultPriceStandard = new BigDecimal("90000");

    @Value("${crawler.default-price.vip:95000}")
    private BigDecimal defaultPriceVip = new BigDecimal("95000");

    @Value("${crawler.default-price.couple:100000}")
    private BigDecimal defaultPriceCouple = new BigDecimal("100000");

    public enum ProcessStatus {
        INSERTED,
        UPDATED,
        SKIPPED,
        FAILED
    }

    /**
     * Executes a full showtime crawl and synchronization batch for a given theater.
     *
     * @param theaterId Target theater ID (default: 1 for CineMind center)
     * @return Summary of execution statistics
     */
    public ShowtimeCrawlerSummaryResponse crawlShowtimes(Long theaterId) {
        log.info("[INFO] Starting showtime crawl batch for theaterId={}", theaterId);
        LocalDateTime startTime = LocalDateTime.now();

        List<ShowtimeItemDTO> rawShowtimes = crawlerClient.fetchShowtimeList();
        int totalFetched = rawShowtimes.size();
        log.info("[INFO] Fetched total={} showtimes from source={}", totalFetched, crawlerClient.getName());

        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;
        int softDeactivated = 0;

        Set<String> seenSourceIds = new HashSet<>();

        // Layer 2: Active Sync — Process each showtime individually
        for (ShowtimeItemDTO item : rawShowtimes) {
            try {
                ProcessStatus status = processShowtimeItem(item, theaterId, startTime);
                switch (status) {
                    case INSERTED -> {
                        inserted++;
                        seenSourceIds.add(item.getSourceId());
                    }
                    case UPDATED -> {
                        updated++;
                        seenSourceIds.add(item.getSourceId());
                    }
                    case SKIPPED -> skipped++;
                    case FAILED -> failed++;
                }
            } catch (Exception e) {
                log.error("[ERROR] Unexpected error processing showtime sourceId='{}': {}",
                        item.getSourceId(), e.getMessage(), e);
                failed++;
            }
        }

        // Layer 1 & 3: Safety Guard & Smart Missing Sync
        List<Showtime> currentActiveNccShowtimes = showtimeRepository.findBySourceIdStartingWithAndIsActiveTrue("ncc:");
        long activeFutureCount = currentActiveNccShowtimes.stream()
                .filter(s -> s.getStartTime().isAfter(startTime))
                .count();

        // Safety guard: If total fetched dropped by > 50% compared to active future count, skip deactivation
        if (activeFutureCount > 10 && totalFetched < (activeFutureCount / 2)) {
            log.warn("[SAFETY_GUARD_TRIGGERED] Crawler fetched only {} showtimes while database has {} active future showtimes. Skipping Smart Missing Sync.",
                    totalFetched, activeFutureCount);
        } else {
            softDeactivated = performSmartMissingSync(currentActiveNccShowtimes, seenSourceIds, startTime);
        }

        LocalDateTime endTime = LocalDateTime.now();
        long executionTimeMs = Duration.between(startTime, endTime).toMillis();

        ShowtimeCrawlerSummaryResponse summary = ShowtimeCrawlerSummaryResponse.builder()
                .totalFetched(totalFetched)
                .inserted(inserted)
                .updated(updated)
                .skipped(skipped)
                .softDeactivated(softDeactivated)
                .failed(failed)
                .startedAt(startTime)
                .finishedAt(endTime)
                .executionTimeMs(executionTimeMs)
                .build();

        log.info("[INFO] Finished showtime crawl batch. Summary: inserted={}, updated={}, skipped={}, softDeactivated={}, failed={}, time={}ms",
                inserted, updated, skipped, softDeactivated, failed, executionTimeMs);

        return summary;
    }

    /**
     * Processes a single ShowtimeItemDTO in a dedicated transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessStatus processShowtimeItem(ShowtimeItemDTO dto, Long theaterId, LocalDateTime now) {
        if (dto == null || dto.getSourceId() == null || dto.getSourceId().isBlank()) {
            return ProcessStatus.SKIPPED;
        }

        // 1. Resolve & Validate Movie
        Optional<Movie> movieOpt = movieRepository.findBySourceId(dto.getFilmSourceId());
        if (movieOpt.isEmpty()) {
            log.warn("[UNMAPPED_MOVIE] Movie not found in CineMind for filmSourceId='{}' title='{}'",
                    dto.getFilmSourceId(), dto.getFilmTitle());
            return ProcessStatus.SKIPPED;
        }

        Movie movie = movieOpt.get();

        // Movie Status validation (RULE-01, RULE-03)
        if (movie.getStatus() == MovieStatus.COMING_SOON || movie.getStatus() == MovieStatus.ENDED) {
            log.warn("[SKIP_MOVIE_STATUS] Movie id={} title='{}' has status={}, skipping showtime sourceId='{}'",
                    movie.getId(), movie.getTitle(), movie.getStatus(), dto.getSourceId());
            return ProcessStatus.SKIPPED;
        }

        // Date validation (RULE-02)
        LocalDate showDate = dto.getStartTime().toLocalDate();
        if (movie.getReleaseDate() != null && showDate.isBefore(movie.getReleaseDate())) {
            log.warn("[INVALID_SHOWTIME_BEFORE_RELEASE_DATE] Showtime startTime={} is before movie releaseDate={} for movie='{}'",
                    dto.getStartTime(), movie.getReleaseDate(), movie.getTitle());
            return ProcessStatus.SKIPPED;
        }

        if (movie.getEndDate() != null && showDate.isAfter(movie.getEndDate())) {
            log.warn("[INVALID_SHOWTIME_AFTER_END_DATE] Showtime startTime={} is after movie endDate={} for movie='{}'",
                    dto.getStartTime(), movie.getEndDate(), movie.getTitle());
            return ProcessStatus.SKIPPED;
        }

        if (dto.getStartTime().isBefore(now)) {
            log.debug("[SKIP_PAST_SHOWTIME] Showtime startTime={} is in the past, skipping", dto.getStartTime());
            return ProcessStatus.SKIPPED;
        }

        // 2. Resolve Room (RULE-07, RULE-08)
        RoomResolver.RoomResolveResult roomResult = roomResolver.resolve(dto.getRoomSourceId(), theaterId);
        if (roomResult.status() != RoomResolver.Status.RESOLVED || roomResult.room() == null) {
            return ProcessStatus.SKIPPED;
        }
        Room room = roomResult.room();

        // 3. Resolve Prices (RULE-10)
        ShowtimePriceParser.PriceResult prices = priceParser.parseAll(
                dto.getPriceStandardRaw(), dto.getPriceVipRaw(), dto.getPriceCoupleRaw());

        BigDecimal priceStandard = prices.standard() != null ? prices.standard() : defaultPriceStandard;
        BigDecimal priceVip = prices.vip() != null ? prices.vip() : defaultPriceVip;
        BigDecimal priceCouple = prices.couple() != null ? prices.couple() : defaultPriceCouple;

        // 4. Idempotency & Upsert (RULE-09, RULE-11)
        Optional<Showtime> existingOpt = showtimeRepository.findBySourceId(dto.getSourceId());

        if (existingOpt.isPresent()) {
            Showtime existing = existingOpt.get();
            existing.setLastSeenAt(now);
            existing.setMissingCount(0);
            existing.setIsOnlineSelling(dto.getIsOnlineSelling() != null ? dto.getIsOnlineSelling() : true);

            if (!existing.getIsActive()) {
                existing.setIsActive(true);
                log.info("[REACTIVATE_SHOWTIME] Showtime id={} sourceId='{}' reactivated",
                        existing.getId(), existing.getSourceId());
            }

            boolean hasReservations = reservationRepository.existsByShowtimeId(existing.getId());
            if (!hasReservations) {
                existing.setPriceStandard(priceStandard);
                existing.setPriceVip(priceVip);
                existing.setPriceCouple(priceCouple);
            }

            showtimeRepository.save(existing);
            return ProcessStatus.UPDATED;
        }

        // Calculate end time
        int duration = (movie.getDuration() != null && movie.getDuration() > 0) ? movie.getDuration() : 120;
        LocalDateTime endTime = dto.getStartTime().plusMinutes(duration);

        // Check room overlap
        LocalDateTime adjustedStart = dto.getStartTime().minusMinutes(bufferTimeMinutes);
        LocalDateTime adjustedEnd = endTime.plusMinutes(bufferTimeMinutes);
        boolean isOverlap = showtimeRepository.existsOverlappingShowtime(room.getId(), adjustedStart, adjustedEnd, null);

        if (isOverlap) {
            log.warn("[OVERLAP_CONFLICT] Overlapping showtime exists in room id={} for time window {} - {}",
                    room.getId(), dto.getStartTime(), endTime);
            return ProcessStatus.SKIPPED;
        }

        // Insert new showtime
        Showtime newShowtime = Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(dto.getStartTime())
                .endTime(endTime)
                .priceStandard(priceStandard)
                .priceVip(priceVip)
                .priceCouple(priceCouple)
                .sourceId(dto.getSourceId())
                .lastSeenAt(now)
                .missingCount(0)
                .isActive(true)
                .isOnlineSelling(dto.getIsOnlineSelling() != null ? dto.getIsOnlineSelling() : true)
                .build();

        showtimeRepository.save(newShowtime);
        log.info("[INFO] Showtime inserted sourceId='{}' movie='{}' room='{}' startTime={}",
                dto.getSourceId(), movie.getTitle(), room.getName(), dto.getStartTime());

        return ProcessStatus.INSERTED;
    }

    /**
     * Layer 3: Performs smart missing synchronization.
     * Future active showtimes not present in the current crawl batch have missingCount incremented.
     * If missingCount >= 3 and 0 bookings, soft deactivate (isActive = false).
     */
    @Transactional
    public int performSmartMissingSync(List<Showtime> activeNccShowtimes, Set<String> seenSourceIds, LocalDateTime now) {
        int softDeactivatedCount = 0;

        for (Showtime st : activeNccShowtimes) {
            if (st.getStartTime().isBefore(now)) {
                continue;
            }

            if (seenSourceIds.contains(st.getSourceId())) {
                continue;
            }

            int currentMissing = st.getMissingCount() != null ? st.getMissingCount() : 0;
            int newMissing = currentMissing + 1;
            st.setMissingCount(newMissing);

            if (newMissing >= 3) {
                boolean hasReservations = reservationRepository.existsByShowtimeId(st.getId());
                if (hasReservations) {
                    log.info("[PROTECTED_SHOWTIME_HAS_BOOKING] Showtime id={} sourceId='{}' missing >= 3 times but has active reservations. Protected.",
                            st.getId(), st.getSourceId());
                } else {
                    st.setIsActive(false);
                    softDeactivatedCount++;
                    log.warn("[SOFT_DEACTIVATED_SHOWTIME] Showtime id={} sourceId='{}' missing {} times with 0 bookings. Soft deactivated.",
                            st.getId(), st.getSourceId(), newMissing);
                }
            }

            showtimeRepository.save(st);
        }

        return softDeactivatedCount;
    }
}
