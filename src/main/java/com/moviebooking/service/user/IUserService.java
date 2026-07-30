package com.moviebooking.service.user;

import com.moviebooking.dto.req.UpdateProfileRequest;
import com.moviebooking.dto.res.UserProfileResponse;

public interface IUserService {
    UserProfileResponse getCurrentUser(String email);

    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);
}
