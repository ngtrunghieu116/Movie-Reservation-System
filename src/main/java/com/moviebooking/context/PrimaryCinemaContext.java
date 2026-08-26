package com.moviebooking.context;

import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.model.Theater;
import com.moviebooking.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Abstraction layer for single-cinema business model.
 * Provides the central primary theater entity and ID without hardcoding across services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrimaryCinemaContext {

    private final TheaterRepository theaterRepository;

    @Value("${app.primary-theater-id:1}")
    private Long primaryTheaterIdConfig;

    /**
     * Resolves the primary theater entity for the system.
     * Uses hybrid strategy: Config ID -> First Active Theater in DB -> Throws exception if DB empty.
     */
    public Theater getPrimaryTheater() {
        if (primaryTheaterIdConfig != null) {
            var theaterOpt = theaterRepository.findById(primaryTheaterIdConfig);
            if (theaterOpt.isPresent()) {
                return theaterOpt.get();
            }
        }

        // Fallback: Find first active theater in database
        return theaterRepository.findByIsActiveTrue().stream()
                .findFirst()
                .orElseGet(() -> theaterRepository.findAll().stream()
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rạp chiếu chính (Primary Theater) trong hệ thống!")));
    }

    /**
     * Resolves the primary theater ID for the system.
     */
    public Long getPrimaryTheaterId() {
        return getPrimaryTheater().getId();
    }
}
