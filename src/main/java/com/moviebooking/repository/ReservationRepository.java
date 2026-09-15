package com.moviebooking.repository;

import com.moviebooking.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserEmailOrderByCreatedAtDesc(String email);
    
    boolean existsByShowtimeId(Long showtimeId);

    boolean existsByUserId(Long userId);

    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    java.util.Optional<Reservation> findByBookingCode(String bookingCode);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Reservation r " +
            "JOIN Payment p ON p.reservation = r " +
            "WHERE r.user.id = :userId AND r.status = 'CONFIRMED' AND p.status = 'COMPLETED' " +
            "ORDER BY r.createdAt DESC")
    List<Reservation> findSuccessfulReservationsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Reservation r WHERE r.status = :pendingStatus AND r.expiresAt IS NOT NULL AND r.expiresAt < :now")
    List<Reservation> findExpiredPendingReservations(@org.springframework.data.repository.query.Param("pendingStatus") com.moviebooking.model.enums.ReservationStatus pendingStatus, @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT DISTINCT r FROM Reservation r " +
                    "LEFT JOIN FETCH r.user u " +
                    "LEFT JOIN FETCH r.showtime s " +
                    "LEFT JOIN FETCH s.movie m " +
                    "LEFT JOIN FETCH s.room rm " +
                    "LEFT JOIN FETCH rm.theater t " +
                    "LEFT JOIN Payment p ON p.reservation = r " +
                    "WHERE (:bookingStatus IS NULL OR r.status = :bookingStatus) " +
                    "AND (:paymentStatus IS NULL OR p.status = :paymentStatus) " +
                    "AND (:startDateTime IS NULL OR s.startTime >= :startDateTime) " +
                    "AND (:endDateTime IS NULL OR s.startTime <= :endDateTime) " +
                    "AND (:search IS NULL OR " +
                    "     LOWER(r.bookingCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     u.phone LIKE CONCAT('%', :search, '%'))",
            countQuery = "SELECT COUNT(DISTINCT r) FROM Reservation r " +
                    "LEFT JOIN r.user u " +
                    "LEFT JOIN r.showtime s " +
                    "LEFT JOIN Payment p ON p.reservation = r " +
                    "WHERE (:bookingStatus IS NULL OR r.status = :bookingStatus) " +
                    "AND (:paymentStatus IS NULL OR p.status = :paymentStatus) " +
                    "AND (:startDateTime IS NULL OR s.startTime >= :startDateTime) " +
                    "AND (:endDateTime IS NULL OR s.startTime <= :endDateTime) " +
                    "AND (:search IS NULL OR " +
                    "     LOWER(r.bookingCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     u.phone LIKE CONCAT('%', :search, '%'))"
    )
    org.springframework.data.domain.Page<Reservation> searchAdminBookings(
            @org.springframework.data.repository.query.Param("bookingStatus") com.moviebooking.model.enums.ReservationStatus bookingStatus,
            @org.springframework.data.repository.query.Param("paymentStatus") com.moviebooking.model.enums.PaymentStatus paymentStatus,
            @org.springframework.data.repository.query.Param("startDateTime") java.time.LocalDateTime startDateTime,
            @org.springframework.data.repository.query.Param("endDateTime") java.time.LocalDateTime endDateTime,
            @org.springframework.data.repository.query.Param("search") String search,
            org.springframework.data.domain.Pageable pageable);

    // =========================================================
    // STATISTICS QUERIES — feature/admin-dashboard-stats
    // =========================================================

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(r.totalPrice), 0) FROM Reservation r " +
        "WHERE r.status = 'CONFIRMED' AND r.createdAt >= :start AND r.createdAt < :end")
    java.math.BigDecimal sumRevenueBetween(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(r.id) FROM Reservation r " +
        "WHERE r.status = 'CONFIRMED' AND r.createdAt >= :start AND r.createdAt < :end")
    Long countConfirmedBetween(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(r.totalPrice), 0) FROM Reservation r " +
        "WHERE r.status = 'CONFIRMED'")
    java.math.BigDecimal sumTotalRevenue();

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(r.id) FROM Reservation r WHERE r.status = 'CONFIRMED'")
    Long countTotalConfirmed();

    // Daily revenue aggregation — returns Object[]{java.sql.Date date, BigDecimal revenue, Long bookingCount}
    @org.springframework.data.jpa.repository.Query(value =
        "SELECT DATE(r.created_at) as stat_date, " +
        "COALESCE(SUM(r.total_price), 0) as revenue, " +
        "COUNT(r.id) as booking_count " +
        "FROM reservations r " +
        "WHERE r.status = 'CONFIRMED' AND r.created_at >= :start AND r.created_at < :end " +
        "GROUP BY DATE(r.created_at) " +
        "ORDER BY stat_date ASC",
        nativeQuery = true)
    List<Object[]> findDailyRevenueBetween(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    // Movie revenue aggregation — returns Object[]{Long movieId, String title, String posterPath, BigDecimal revenue, Long bookingCount}
    @org.springframework.data.jpa.repository.Query(value =
        "SELECT m.id, m.title, m.poster_path, " +
        "COALESCE(SUM(r.total_price), 0) as revenue, " +
        "COUNT(r.id) as booking_count " +
        "FROM reservations r " +
        "JOIN showtimes s ON r.showtime_id = s.id " +
        "JOIN movies m ON s.movie_id = m.id " +
        "WHERE r.status = 'CONFIRMED' " +
        "AND (:start IS NULL OR r.created_at >= :start) " +
        "AND (:end IS NULL OR r.created_at < :end) " +
        "GROUP BY m.id, m.title, m.poster_path " +
        "ORDER BY revenue DESC, m.id ASC " +
        "LIMIT :lim",
        nativeQuery = true)
    List<Object[]> findMovieRevenueBetween(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end,
        @org.springframework.data.repository.query.Param("lim") int limit);

    // Room performance aggregation — Object[]{Long roomId, String roomName, String roomType, Long showtimes, BigDecimal revenue}
    @org.springframework.data.jpa.repository.Query(value =
        "SELECT rm.id, rm.name, rm.room_type, " +
        "COUNT(DISTINCT s.id) as showtimes_count, " +
        "COALESCE(SUM(r.total_price), 0) as revenue " +
        "FROM rooms rm " +
        "LEFT JOIN showtimes s ON s.room_id = rm.id " +
        "LEFT JOIN reservations r ON r.showtime_id = s.id AND r.status = 'CONFIRMED' " +
        "AND (:start IS NULL OR r.created_at >= :start) " +
        "AND (:end IS NULL OR r.created_at < :end) " +
        "GROUP BY rm.id, rm.name, rm.room_type " +
        "ORDER BY revenue DESC",
        nativeQuery = true)
    List<Object[]> findRoomPerformanceBetween(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    // Count showtimes in date range
    @org.springframework.data.jpa.repository.Query(value =
        "SELECT COUNT(DISTINCT s.id) FROM showtimes s " +
        "WHERE s.start_time >= :start AND s.start_time < :end",
        nativeQuery = true)
    Long countShowtimesBetween(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    // Count active movies in date range (movies with at least one showtime)
    @org.springframework.data.jpa.repository.Query(value =
        "SELECT COUNT(DISTINCT s.movie_id) FROM showtimes s " +
        "WHERE s.start_time >= :start AND s.start_time < :end",
        nativeQuery = true)
    Long countActiveMoviesBetween(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);
}
