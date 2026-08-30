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
}
