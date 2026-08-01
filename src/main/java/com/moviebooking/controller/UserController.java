package com.moviebooking.controller;

import com.moviebooking.dto.req.ChangePasswordRequest;
import com.moviebooking.dto.req.UpdateProfileRequest;
import com.moviebooking.dto.res.TransactionHistoryResponse;
import com.moviebooking.dto.res.UserProfileResponse;
import com.moviebooking.security.CustomUserDetails;
import com.moviebooking.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // API lay thong tin user
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getCurrentUser(userDetails.getUsername()));
    }

    // API cap nhat thong tin
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userDetails.getUsername(), request));
    }

    // API lay lich su giao dich
    @GetMapping("/me/transactions")
    public ResponseEntity<List<TransactionHistoryResponse>> getTransactionHistory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getTransactionHistory(userDetails.getUsername()));
    }
}
