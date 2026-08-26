package com.moviebooking.context;

import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.model.Theater;
import com.moviebooking.repository.TheaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrimaryCinemaContextTest {

    @Mock
    private TheaterRepository theaterRepository;

    @InjectMocks
    private PrimaryCinemaContext primaryCinemaContext;

    private Theater sampleTheater;

    @BeforeEach
    void setUp() {
        sampleTheater = Theater.builder()
                .id(1L)
                .name("Trung tâm Chiếu phim Quốc gia")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should resolve primary theater by configured ID when present in DB")
    void testGetPrimaryTheater_ConfigIdFound() {
        ReflectionTestUtils.setField(primaryCinemaContext, "primaryTheaterIdConfig", 1L);
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(sampleTheater));

        Theater theater = primaryCinemaContext.getPrimaryTheater();

        assertNotNull(theater);
        assertEquals(1L, theater.getId());
        assertEquals("Trung tâm Chiếu phim Quốc gia", theater.getName());
        verify(theaterRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should fallback to first active theater when configured ID is not found")
    void testGetPrimaryTheater_FallbackToActiveTheater() {
        ReflectionTestUtils.setField(primaryCinemaContext, "primaryTheaterIdConfig", 99L);
        when(theaterRepository.findById(99L)).thenReturn(Optional.empty());
        when(theaterRepository.findByIsActiveTrue()).thenReturn(List.of(sampleTheater));

        Theater theater = primaryCinemaContext.getPrimaryTheater();

        assertNotNull(theater);
        assertEquals(1L, theater.getId());
        verify(theaterRepository, times(1)).findById(99L);
        verify(theaterRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when no theater exists in DB")
    void testGetPrimaryTheater_NotFoundThrowsException() {
        ReflectionTestUtils.setField(primaryCinemaContext, "primaryTheaterIdConfig", 1L);
        when(theaterRepository.findById(1L)).thenReturn(Optional.empty());
        when(theaterRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());
        when(theaterRepository.findAll()).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> primaryCinemaContext.getPrimaryTheater());
    }

    @Test
    @DisplayName("Should return correct primary theater ID")
    void testGetPrimaryTheaterId() {
        ReflectionTestUtils.setField(primaryCinemaContext, "primaryTheaterIdConfig", 1L);
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(sampleTheater));

        Long theaterId = primaryCinemaContext.getPrimaryTheaterId();

        assertEquals(1L, theaterId);
    }
}
