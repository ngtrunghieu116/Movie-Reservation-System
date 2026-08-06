package com.moviebooking.service.user;

import com.moviebooking.dto.req.AdminUserRequest;
import com.moviebooking.dto.req.UpdateProfileRequest;
import com.moviebooking.dto.res.AdminUserResponse;
import com.moviebooking.dto.res.TransactionHistoryResponse;
import com.moviebooking.dto.res.UserProfileResponse;
import com.moviebooking.model.enums.Role;
import com.moviebooking.model.enums.UserStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IUserService {
    UserProfileResponse getCurrentUser(String email);

    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);

    List<TransactionHistoryResponse> getTransactionHistory(String email);

    // Admin methods
    Page<AdminUserResponse> searchUsers(Role role, UserStatus status, String search, int page, int size);

    AdminUserResponse getUserById(Long id);

    AdminUserResponse adminUpdateUser(Long id, AdminUserRequest request);

    void adminResetPassword(Long id, String newPassword);

    void deleteUser(Long id);
}
