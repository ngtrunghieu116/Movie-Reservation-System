package com.moviebooking.repository;

import com.moviebooking.model.EmailVerificationToken;
import com.moviebooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    // Tìm token record theo chuỗi token
    Optional<EmailVerificationToken> findByToken(String token);

    // Tìm tất cả token chưa dùng của một user (để vô hiệu hóa token cũ khi gửi lại)
    List<EmailVerificationToken> findByUserAndUsedFalse(User user);
}
