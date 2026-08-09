package com.moviebooking.service.movie;

import com.moviebooking.model.Movie;
import com.moviebooking.model.enums.MovieStatus;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.service.movie.resolver.MovieStatusResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MovieStatusServiceTest {

    private MovieRepository movieRepository;
    private MovieStatusResolver statusResolver;
    private MovieStatusService movieStatusService;

    @BeforeEach
    void setUp() {
        movieRepository = mock(MovieRepository.class);
        statusResolver = mock(MovieStatusResolver.class);
        movieStatusService = new MovieStatusService(movieRepository, statusResolver);
    }

    @Test
    void updateAllStatuses_WhenStatusChanges_ShouldSaveAndReturnUpdatedCount() {
        Movie movie1 = Movie.builder()
                .id(1L)
                .title("Movie 1")
                .releaseDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .status(MovieStatus.COMING_SOON)
                .build();

        when(movieRepository.findByStatusIn(anyList())).thenReturn(List.of(movie1));
        when(statusResolver.resolveStatus(eq(movie1.getReleaseDate()), eq(movie1.getEndDate()), any(LocalDate.class)))
                .thenReturn(MovieStatus.NOW_SHOWING);

        int updatedCount = movieStatusService.updateAllStatuses();

        assertEquals(1, updatedCount);
        assertEquals(MovieStatus.NOW_SHOWING, movie1.getStatus());
        verify(movieRepository, times(1)).save(movie1);
    }

    @Test
    void updateAllStatuses_WhenStatusDoesNotChange_ShouldNotSave() {
        Movie movie2 = Movie.builder()
                .id(2L)
                .title("Movie 2")
                .releaseDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(5))
                .status(MovieStatus.NOW_SHOWING)
                .build();

        when(movieRepository.findByStatusIn(anyList())).thenReturn(List.of(movie2));
        when(statusResolver.resolveStatus(eq(movie2.getReleaseDate()), eq(movie2.getEndDate()), any(LocalDate.class)))
                .thenReturn(MovieStatus.NOW_SHOWING);

        int updatedCount = movieStatusService.updateAllStatuses();

        assertEquals(0, updatedCount);
        assertEquals(MovieStatus.NOW_SHOWING, movie2.getStatus());
        verify(movieRepository, never()).save(movie2);
    }
}
