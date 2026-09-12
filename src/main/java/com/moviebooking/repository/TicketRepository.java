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

    // =========================================================
    // STATISTICS QUERIES — feature/admin-dashboard-stats
    // =========================================================

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(t.id) FROM Ticket t " +
        "WHERE t.reservation.status = 'CONFIRMED' AND t.status != 'CANCELLED' " +
        "AND t.createdAt >= :start AND t.createdAt < :end")
    Long countTicketsSoldBetween(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(t.id) FROM Ticket t " +
        "WHERE t.reservation.status = 'CONFIRMED' AND t.status != 'CANCELLED'")
    Long countTotalTicketsSold();

    // Returns Object[]{java.sql.Date date, Long ticket_count}
    @org.springframework.data.jpa.repository.Query(value =
        "SELECT DATE(t.created_at) as stat_date, COUNT(t.id) as ticket_count " +
        "FROM tickets t " +
        "JOIN reservations r ON t.reservation_id = r.id " +
        "WHERE r.status = 'CONFIRMED' AND t.status != 'CANCELLED' " +
        "AND t.created_at >= :start AND t.created_at < :end " +
        "GROUP BY DATE(t.created_at) " +
        "ORDER BY stat_date ASC",
        nativeQuery = true)
    List<Object[]> countDailyTicketsBetween(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    // Returns Object[]{Long movieId, Long ticket_count}
    @org.springframework.data.jpa.repository.Query(value =
        "SELECT s.movie_id, COUNT(t.id) as ticket_count " +
        "FROM tickets t " +
        "JOIN showtimes s ON t.showtime_id = s.id " +
        "JOIN reservations r ON t.reservation_id = r.id " +
        "WHERE r.status = 'CONFIRMED' AND t.status != 'CANCELLED' " +
        "AND (:start IS NULL OR t.created_at >= :start) " +
        "AND (:end IS NULL OR t.created_at < :end) " +
        "GROUP BY s.movie_id",
        nativeQuery = true)
    List<Object[]> countTicketsByMovieBetween(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    // Returns Object[]{Long roomId, Long ticket_count} per room
    @org.springframework.data.jpa.repository.Query(value =
        "SELECT rm.id, COUNT(t.id) as ticket_count " +
        "FROM tickets t " +
        "JOIN showtimes s ON t.showtime_id = s.id " +
        "JOIN rooms rm ON s.room_id = rm.id " +
        "JOIN reservations r ON t.reservation_id = r.id " +
        "WHERE r.status = 'CONFIRMED' AND t.status != 'CANCELLED' " +
        "AND (:start IS NULL OR t.created_at >= :start) " +
        "AND (:end IS NULL OR t.created_at < :end) " +
        "GROUP BY rm.id",
        nativeQuery = true)
    List<Object[]> countTicketsByRoomBetween(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

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
