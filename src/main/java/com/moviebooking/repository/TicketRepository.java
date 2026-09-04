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

    Optional<Ticket> findByReservationIdAndSeatId(Long reservationId, Long seatId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.ticketCode = :ticketCode")
    Optional<Ticket> findByTicketCodeWithLock(@org.springframework.data.repository.query.Param("ticketCode") String ticketCode);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) > 0 FROM Ticket t " +
            "WHERE t.reservation.user.id = :userId " +
            "AND t.showtime.movie.id = :movieId " +
            "AND t.reservation.status = :reservationStatus " +
            "AND t.status IN :ticketStatuses")
    boolean existsVerifiedTicketForUserAndMovie(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("movieId") Long movieId,
            @org.springframework.data.repository.query.Param("reservationStatus") com.moviebooking.model.enums.ReservationStatus reservationStatus,
            @org.springframework.data.repository.query.Param("ticketStatuses") java.util.Collection<TicketStatus> ticketStatuses);
}
