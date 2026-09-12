package com.moviebooking.repository;

import com.moviebooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moviebooking.model.enums.Role;
import com.moviebooking.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Tìm User theo email (dùng cho Đăng nhập)
    Optional<User> findByEmail(String email);

    // Kiểm tra email đã tồn tại chưa (dùng cho Đăng ký)
    boolean existsByEmail(String email);

    // Kiểm tra số điện thoại đã tồn tại chưa (dùng cho Đăng ký)
    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "u.phone LIKE CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("role") Role role,
                           @Param("status") UserStatus status,
                           @Param("search") String search,
                           Pageable pageable);

    // =========================================================
    // STATISTICS QUERIES — feature/admin-dashboard-stats
    // =========================================================

    @Query("SELECT COUNT(u.id) FROM User u WHERE u.role = 'USER' " +
           "AND u.createdAt >= :start AND u.createdAt < :end")
    Long countNewUsersBetween(
        @Param("start") java.time.LocalDateTime start,
        @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COUNT(u.id) FROM User u WHERE u.role = 'USER'")
    Long countTotalUsers();

    // Returns Object[]{java.sql.Date date, Long new_users}
    @org.springframework.data.jpa.repository.Query(value =
        "SELECT DATE(u.created_at) as stat_date, COUNT(u.id) as new_users " +
        "FROM users u " +
        "WHERE u.role = 'USER' " +
        "AND u.created_at >= :start AND u.created_at < :end " +
        "GROUP BY DATE(u.created_at) " +
        "ORDER BY stat_date ASC",
        nativeQuery = true)
    List<Object[]> countDailyNewUsersBetween(
        @Param("start") java.time.LocalDateTime start,
        @Param("end") java.time.LocalDateTime end);
}

