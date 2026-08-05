package com.moviebooking.repository;

import com.moviebooking.model.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    boolean existsByRoomIdAndEndTimeAfter(Long roomId, LocalDateTime time);
}
