package com.moviebooking.service.user;

import com.moviebooking.dto.req.UpdateProfileRequest;
import com.moviebooking.dto.res.TransactionHistoryResponse;
import com.moviebooking.dto.res.UserProfileResponse;

import java.util.List;

public interface IUserService {
    UserProfileResponse getCurrentUser(String email);

    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);

    List<TransactionHistoryResponse> getTransactionHistory(String email);
}
