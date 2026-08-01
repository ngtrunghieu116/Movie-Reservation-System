package com.moviebooking.repository;

import com.moviebooking.model.PasswordResetToken;
import com.moviebooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    // Tìm token record theo chuỗi JWT token
    Optional<PasswordResetToken> findByToken(String token);

    // Tìm tất cả token chưa dùng của một user (để vô hiệu hóa token cũ khi yêu cầu mới)
    List<PasswordResetToken> findByUserAndUsedFalse(User user);
}
