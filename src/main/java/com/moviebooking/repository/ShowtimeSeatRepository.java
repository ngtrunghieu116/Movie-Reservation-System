package com.moviebooking.repository;

import com.moviebooking.model.ShowtimeSeat;
import com.moviebooking.model.enums.ShowtimeSeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {

    List<ShowtimeSeat> findByShowtimeIdOrderBySeatRowNameAscSeatSeatNumberAsc(Long showtimeId);

    Optional<ShowtimeSeat> findByShowtimeIdAndSeatId(Long showtimeId, Long seatId);

    List<ShowtimeSeat> findByShowtimeIdAndSeatIdIn(Long showtimeId, List<Long> seatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowtimeSeat ss WHERE ss.showtime.id = :showtimeId AND ss.seat.id IN :seatIds")
    List<ShowtimeSeat> findByShowtimeIdAndSeatIdInWithLock(
            @Param("showtimeId") Long showtimeId,
            @Param("seatIds") List<Long> seatIds
    );

    List<ShowtimeSeat> findByReservationId(Long reservationId);

    boolean existsByShowtimeId(Long showtimeId);

    long countByShowtimeIdAndStatus(Long showtimeId, ShowtimeSeatStatus status);

    @Query("SELECT ss FROM ShowtimeSeat ss WHERE ss.status = 'HELD' AND ss.lockedUntil < :now")
    List<ShowtimeSeat> findExpiredHeldSeats(@Param("now") LocalDateTime now);
}
