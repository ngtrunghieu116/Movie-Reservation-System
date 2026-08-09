package com.moviebooking.crawler.resolver;

import com.moviebooking.model.Genre;
import com.moviebooking.repository.GenreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreResolverTest {

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreResolver genreResolver;

    @BeforeEach
    void setUp() {
    }

    @Test
    void resolve_WithMultipleUniqueGenres_ShouldReturnAll() {
        Genre g1 = new Genre(); g1.setId(1L); g1.setName("Hành động");
        Genre g2 = new Genre(); g2.setId(2L); g2.setName("Viễn tưởng");
        when(genreRepository.findByNameIgnoreCase("Hành động")).thenReturn(Optional.of(g1));
        when(genreRepository.findByNameIgnoreCase("Viễn tưởng")).thenReturn(Optional.of(g2));

        Set<Genre> genres = genreResolver.resolve("Hành động, Viễn tưởng");

        assertEquals(2, genres.size());
        verify(genreRepository, times(1)).findByNameIgnoreCase("Hành động");
        verify(genreRepository, times(1)).findByNameIgnoreCase("Viễn tưởng");
    }

    @Test
    void resolve_WithDuplicateGenres_ShouldReturnUnique() {
        Genre g1 = new Genre(); g1.setId(1L); g1.setName("Hành động");
        when(genreRepository.findByNameIgnoreCase("Hành động")).thenReturn(Optional.of(g1));

        // Note: The resolve method handles deduplication before hitting the DB due to distinct mapping using Set
        Set<Genre> genres = genreResolver.resolve("Hành động, hành động, HÀNH ĐỘNG");

        assertEquals(1, genres.size());
        assertEquals("Hành động", genres.iterator().next().getName());
        verify(genreRepository, times(1)).findByNameIgnoreCase("Hành động");
    }

    @Test
    void resolve_WithNewGenre_ShouldSaveAndReturn() {
        when(genreRepository.findByNameIgnoreCase("Hài hước")).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> {
            Genre g = invocation.getArgument(0);
            g.setId(10L);
            return g;
        });

        Set<Genre> genres = genreResolver.resolve("Hài hước");

        assertEquals(1, genres.size());
        Genre resolved = genres.iterator().next();
        assertEquals("Hài hước", resolved.getName());
        assertEquals(10L, resolved.getId());
        verify(genreRepository, times(1)).save(any(Genre.class));
    }

    @Test
    void resolve_WithNullOrEmpty_ShouldReturnEmptySet() {
        assertTrue(genreResolver.resolve(null).isEmpty());
        assertTrue(genreResolver.resolve("").isEmpty());
        assertTrue(genreResolver.resolve("   ").isEmpty());
    }
}
