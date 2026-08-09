package com.moviebooking.service.movie.resolver;

import com.moviebooking.model.enums.MovieStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovieStatusResolverTest {

    private MovieStatusResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new MovieStatusResolver();
    }

    @Test
    void resolveStatus_TargetDateBeforeReleaseDate_ShouldReturnComingSoon() {
        LocalDate releaseDate = LocalDate.of(2026, 8, 10);
        LocalDate endDate = LocalDate.of(2026, 9, 10);
        LocalDate targetDate = LocalDate.of(2026, 8, 9);

        MovieStatus status = resolver.resolveStatus(releaseDate, endDate, targetDate);

        assertEquals(MovieStatus.COMING_SOON, status);
    }

    @Test
    void resolveStatus_TargetDateEqualsReleaseDate_ShouldReturnNowShowing() {
        LocalDate releaseDate = LocalDate.of(2026, 8, 10);
        LocalDate endDate = LocalDate.of(2026, 9, 10);
        LocalDate targetDate = LocalDate.of(2026, 8, 10);

        MovieStatus status = resolver.resolveStatus(releaseDate, endDate, targetDate);

        assertEquals(MovieStatus.NOW_SHOWING, status);
    }

    @Test
    void resolveStatus_TargetDateBetweenReleaseDateAndEndDate_ShouldReturnNowShowing() {
        LocalDate releaseDate = LocalDate.of(2026, 8, 10);
        LocalDate endDate = LocalDate.of(2026, 9, 10);
        LocalDate targetDate = LocalDate.of(2026, 8, 15);

        MovieStatus status = resolver.resolveStatus(releaseDate, endDate, targetDate);

        assertEquals(MovieStatus.NOW_SHOWING, status);
    }

    @Test
    void resolveStatus_TargetDateEqualsEndDate_ShouldReturnEnded() {
        LocalDate releaseDate = LocalDate.of(2026, 8, 10);
        LocalDate endDate = LocalDate.of(2026, 9, 10);
        LocalDate targetDate = LocalDate.of(2026, 9, 10);

        MovieStatus status = resolver.resolveStatus(releaseDate, endDate, targetDate);

        assertEquals(MovieStatus.ENDED, status);
    }

    @Test
    void resolveStatus_TargetDateAfterEndDate_ShouldReturnEnded() {
        LocalDate releaseDate = LocalDate.of(2026, 8, 10);
        LocalDate endDate = LocalDate.of(2026, 9, 10);
        LocalDate targetDate = LocalDate.of(2026, 9, 11);

        MovieStatus status = resolver.resolveStatus(releaseDate, endDate, targetDate);

        assertEquals(MovieStatus.ENDED, status);
    }

    @Test
    void resolveStatus_ReleaseDateNull_ShouldReturnComingSoon() {
        MovieStatus status = resolver.resolveStatus(null, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 8, 10));

        assertEquals(MovieStatus.COMING_SOON, status);
    }

    @Test
    void resolveStatus_EndDateNullTargetDateAfterReleaseDate_ShouldReturnNowShowing() {
        LocalDate releaseDate = LocalDate.of(2026, 8, 10);
        LocalDate targetDate = LocalDate.of(2026, 8, 15);

        MovieStatus status = resolver.resolveStatus(releaseDate, null, targetDate);

        assertEquals(MovieStatus.NOW_SHOWING, status);
    }
}
