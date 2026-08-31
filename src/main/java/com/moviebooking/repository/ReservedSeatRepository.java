package com.moviebooking.repository;

import com.moviebooking.model.ReservedSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservedSeatRepository extends JpaRepository<ReservedSeat, Long> {
    int countByReservationId(Long reservationId);

    List<ReservedSeat> findByReservationId(Long reservationId);

    List<ReservedSeat> findByReservationIdIn(List<Long> reservationIds);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(rs) FROM ReservedSeat rs WHERE rs.reservation.showtime.id = :showtimeId AND rs.reservation.status != 'CANCELLED'")
    long countBookedSeatsByShowtimeId(@org.springframework.data.repository.query.Param("showtimeId") Long showtimeId);
}

