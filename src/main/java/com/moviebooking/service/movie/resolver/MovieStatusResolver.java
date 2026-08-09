package com.moviebooking.service.movie.resolver;

import com.moviebooking.model.enums.MovieStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Pure logic resolver to determine MovieStatus based on dates.
 * Does not depend on repositories, external services, or current system time.
 */
@Component
public class MovieStatusResolver {

    /**
     * Resolves MovieStatus based on releaseDate, endDate, and targetDate.
     *
     * Rules:
     * - releaseDate > targetDate           -> COMING_SOON
     * - releaseDate <= targetDate < endDate -> NOW_SHOWING
     * - targetDate >= endDate               -> ENDED
     *
     * @param releaseDate Movie release date
     * @param endDate Movie end date
     * @param targetDate The reference date to evaluate against (e.g. today's date)
     * @return Calculated MovieStatus
     */
    public MovieStatus resolveStatus(LocalDate releaseDate, LocalDate endDate, LocalDate targetDate) {
        if (releaseDate == null || releaseDate.isAfter(targetDate)) {
            return MovieStatus.COMING_SOON;
        }
        if (endDate != null && !targetDate.isBefore(endDate)) {
            return MovieStatus.ENDED;
        }
        return MovieStatus.NOW_SHOWING;
    }
}
