package com.moviebooking.repository;

import com.moviebooking.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByRoomIdOrderByRowNameAscSeatNumberAsc(Long roomId);

    long countByRoomId(Long roomId);

    boolean existsByRoomId(Long roomId);

    void deleteByRoomId(Long roomId);
}
