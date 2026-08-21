package com.moviebooking.service.showtime;

import com.moviebooking.dto.req.ShowtimeRequest;
import com.moviebooking.model.Movie;
import com.moviebooking.model.Room;
import com.moviebooking.model.enums.MovieStatus;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.RoomRepository;
import com.moviebooking.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceValidationTest {

    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private ShowtimeService showtimeService;

    private Room activeRoom;
    private ShowtimeRequest request;

    @BeforeEach
    void setUp() {
        activeRoom = new Room();
        activeRoom.setId(1L);
        activeRoom.setIsActive(true);

        request = new ShowtimeRequest();
        request.setRoomId(1L);
        request.setMovieId(10L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setPriceStandard(BigDecimal.valueOf(100000));
        request.setPriceVip(BigDecimal.valueOf(120000));
        request.setPriceCouple(BigDecimal.valueOf(200000));
    }

    @Test
    void createShowtime_WithComingSoonMovie_ShouldThrowException() {
        Movie comingSoonMovie = new Movie();
        comingSoonMovie.setId(10L);
        comingSoonMovie.setStatus(MovieStatus.COMING_SOON);
        comingSoonMovie.setReleaseDate(LocalDate.now().plusDays(5));

        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeRoom));
        when(movieRepository.findById(10L)).thenReturn(Optional.of(comingSoonMovie));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            showtimeService.createShowtime(request);
        });

        assertTrue(ex.getMessage().contains("COMING_SOON"));
        verify(showtimeRepository, never()).save(any());
    }

    @Test
    void createShowtime_WithEndedMovie_ShouldThrowException() {
        Movie endedMovie = new Movie();
        endedMovie.setId(10L);
        endedMovie.setStatus(MovieStatus.ENDED);

        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeRoom));
        when(movieRepository.findById(10L)).thenReturn(Optional.of(endedMovie));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            showtimeService.createShowtime(request);
        });

        assertTrue(ex.getMessage().contains("ENDED"));
        verify(showtimeRepository, never()).save(any());
    }
}
