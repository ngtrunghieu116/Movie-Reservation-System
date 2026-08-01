package com.moviebooking.repository;

import com.moviebooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Tìm User theo email (dùng cho Đăng nhập)
    Optional<User> findByEmail(String email);

    // Kiểm tra email đã tồn tại chưa (dùng cho Đăng ký)
    boolean existsByEmail(String email);

    // Kiểm tra số điện thoại đã tồn tại chưa (dùng cho Đăng ký)
    boolean existsByPhone(String phone);
}
