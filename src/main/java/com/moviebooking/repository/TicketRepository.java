package com.moviebooking.repository;

import com.moviebooking.model.Ticket;
import com.moviebooking.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByReservationId(Long reservationId);

    List<Ticket> findByReservationUserIdOrderByCreatedAtDesc(Long userId);

    List<Ticket> findByShowtimeIdAndStatus(Long showtimeId, TicketStatus status);

    boolean existsByTicketCode(String ticketCode);
}
