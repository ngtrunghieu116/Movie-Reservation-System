package com.moviebooking.repository;

import com.moviebooking.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserEmailOrderByCreatedAtDesc(String email);
    
    boolean existsByShowtimeId(Long showtimeId);
}
