package com.moviebooking.controller;

import com.moviebooking.dto.req.ChangePasswordRequest;
import com.moviebooking.dto.req.LoginRequest;
import com.moviebooking.dto.req.RegisterRequest;
import com.moviebooking.dto.req.ResetPasswordRequest;
import com.moviebooking.dto.res.AuthResponse;
import com.moviebooking.security.CustomUserDetails;
import com.moviebooking.service.auth.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final IAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // API doi mat khau
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok("Đổi mật khẩu thành công!");
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(
            @Valid @RequestBody com.moviebooking.dto.req.GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // Trong mô hình JWT stateless, việc đăng xuất thực chất là client xóa token
        // khỏi local storage.
        // API này dùng để xác nhận thành công cho Frontend và sẵn sàng mở rộng (ví dụ
        // Redis blacklist token).
        return ResponseEntity.ok("Đăng xuất thành công!");
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        try {
            authService.verifyEmail(token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/login?verified=true")
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location",
                            "http://localhost:3000/login?error=" + java.net.URLEncoder.encode(e.getMessage(),
                                    java.nio.charset.StandardCharsets.UTF_8))
                    .build();
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerificationEmail(@RequestParam String email) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok("Email xác thực đã được gửi lại thành công.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody com.moviebooking.dto.req.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok("Nếu email tồn tại, link reset mật khẩu đã được gửi!");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Mật khẩu đã được thay đổi thành công!");
    }
}
