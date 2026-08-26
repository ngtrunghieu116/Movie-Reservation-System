package com.moviebooking.security;

import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.model.User;
import com.moviebooking.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves the current authenticated user from SecurityContext.
     * Throws AccessDeniedException if not authenticated.
     * Throws ResourceNotFoundException if user record does not exist.
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Người dùng chưa đăng nhập hoặc phiên làm việc đã hết hạn");
        }

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));
    }
}
