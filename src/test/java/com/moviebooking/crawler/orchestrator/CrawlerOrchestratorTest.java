package com.moviebooking.crawler.orchestrator;

import com.moviebooking.crawler.client.CrawlerClient;
import com.moviebooking.crawler.dto.MovieDetailDTO;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import com.moviebooking.crawler.enricher.MovieEnricher;
import com.moviebooking.crawler.mapper.MovieMapper;
import com.moviebooking.crawler.validator.BusinessValidator;
import com.moviebooking.crawler.validator.DtoValidator;
import com.moviebooking.model.Genre;
import com.moviebooking.model.Movie;
import com.moviebooking.model.enums.MovieStatus;
import com.moviebooking.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlerOrchestratorTest {

    @Mock
    private CrawlerClient crawlerClient;
    @Mock
    private DtoValidator dtoValidator;
    @Mock
    private BusinessValidator businessValidator;
    @Mock
    private MovieMapper movieMapper;
    @Mock
    private MovieEnricher movieEnricher;
    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private CrawlerOrchestrator orchestrator;

    private MovieListItemDTO listItem;
    private MovieDetailDTO detail;

    @BeforeEach
    void setUp() {
        listItem = MovieListItemDTO.builder()
                .sourceId("ncc:1001")
                .title("Test Movie")
                .detailUrl("ncc:1001")
                .releaseDate(LocalDate.now().plusDays(10)) // Coming Soon
                .build();

        detail = MovieDetailDTO.builder()
                .description("Test Intro")
                .genres(List.of("Hành động"))
                .build();
    }

    @Test
    @DisplayName("Process NEW Movie -> Should insert movie into DB")
    void processMovie_NewMovie_ShouldInsertSuccessfully() {
        when(crawlerClient.fetchMovieDetail(any())).thenReturn(detail);
        when(movieRepository.findBySourceId("ncc:1001")).thenReturn(Optional.empty());
        
        Movie movie = new Movie();
        movie.setSourceId("ncc:1001");
        movie.setTitle("Test Movie");
        movie.setStatus(MovieStatus.COMING_SOON);

        when(movieMapper.toEntity(any(), any(), any())).thenReturn(movie);
        when(crawlerClient.getName()).thenReturn("NCC");

        boolean result = orchestrator.processMovie(listItem);

        assertTrue(result, "Processing NEW movie should return true");
        verify(movieRepository, times(1)).save(movie);
        verify(movieEnricher, times(1)).enrich(movie, listItem, detail);
    }

    @Test
    @DisplayName("Process EXISTING Movie with empty genres -> Should supplement missing genres")
    void processMovie_ExistingMovie_WithEmptyGenres_ShouldSupplementGenres() {
        when(crawlerClient.fetchMovieDetail(any())).thenReturn(detail);

        Movie existingMovie = new Movie();
        existingMovie.setSourceId("ncc:1001");
        existingMovie.setTitle("Test Movie");
        existingMovie.setGenres(new HashSet<>()); // Empty genres

        Genre actionGenre = Genre.builder().id(1L).name("Hành động").build();

        when(movieRepository.findBySourceId("ncc:1001")).thenReturn(Optional.of(existingMovie));
        when(movieEnricher.enrichExistingMovie(existingMovie, listItem, detail)).thenAnswer(invocation -> {
            existingMovie.setGenres(Set.of(actionGenre));
            return true;
        });

        boolean result = orchestrator.processMovie(listItem);

        assertTrue(result, "Supplementing missing genres should return true");
        verify(movieRepository, times(1)).save(existingMovie);
        assertEquals(1, existingMovie.getGenres().size());
    }

    @Test
    @DisplayName("Process EXISTING Movie with genres already present -> Should skip without overwrite")
    void processMovie_ExistingMovie_WithGenresPresent_ShouldSkip() {
        when(crawlerClient.fetchMovieDetail(any())).thenReturn(detail);

        Genre actionGenre = Genre.builder().id(1L).name("Hành động").build();

        Movie existingMovie = new Movie();
        existingMovie.setSourceId("ncc:1001");
        existingMovie.setTitle("Test Movie");
        existingMovie.setGenres(Set.of(actionGenre));

        when(movieRepository.findBySourceId("ncc:1001")).thenReturn(Optional.of(existingMovie));
        when(movieEnricher.enrichExistingMovie(existingMovie, listItem, detail)).thenReturn(false);

        boolean result = orchestrator.processMovie(listItem);

        assertFalse(result, "Existing movie with complete data should be skipped (return false)");
        verify(movieRepository, never()).save(any());
    }
}
