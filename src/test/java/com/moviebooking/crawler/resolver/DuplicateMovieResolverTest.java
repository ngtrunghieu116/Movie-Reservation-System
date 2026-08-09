package com.moviebooking.crawler.resolver;

import com.moviebooking.model.Movie;
import com.moviebooking.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuplicateMovieResolverTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private DuplicateMovieResolver duplicateMovieResolver;

    @BeforeEach
    void setUp() {
    }

    @Test
    void isDuplicate_WhenSourceIdExists_ShouldReturnTrue() {
        String sourceId = "ncc:123";
        com.moviebooking.crawler.dto.MovieListItemDTO item = new com.moviebooking.crawler.dto.MovieListItemDTO();
        item.setSourceId(sourceId);
        when(movieRepository.existsBySourceId(sourceId)).thenReturn(true);

        DuplicateMovieResolver.Status result = duplicateMovieResolver.checkDuplicate(item);

        assertEquals(DuplicateMovieResolver.Status.EXISTING, result);
        verify(movieRepository, times(1)).existsBySourceId(sourceId);
    }

    @Test
    void isDuplicate_WhenSourceIdDoesNotExist_ShouldReturnFalse() {
        String sourceId = "ncc:456";
        com.moviebooking.crawler.dto.MovieListItemDTO item = new com.moviebooking.crawler.dto.MovieListItemDTO();
        item.setSourceId(sourceId);
        when(movieRepository.existsBySourceId(sourceId)).thenReturn(false);

        DuplicateMovieResolver.Status result = duplicateMovieResolver.checkDuplicate(item);

        assertEquals(DuplicateMovieResolver.Status.NEW, result);
        verify(movieRepository, times(1)).existsBySourceId(sourceId);
    }
}
