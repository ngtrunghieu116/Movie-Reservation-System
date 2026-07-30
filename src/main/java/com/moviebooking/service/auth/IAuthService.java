package com.moviebooking.service.auth;

import com.moviebooking.dto.req.ChangePasswordRequest;
import com.moviebooking.dto.req.ForgotPasswordRequest;
import com.moviebooking.dto.req.GoogleLoginRequest;
import com.moviebooking.dto.req.LoginRequest;
import com.moviebooking.dto.req.RegisterRequest;
import com.moviebooking.dto.req.ResetPasswordRequest;
import com.moviebooking.dto.res.AuthResponse;

public interface IAuthService {
    String register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse googleLogin(GoogleLoginRequest request);

    void changePassword(String email, ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void verifyEmail(String token);
}
