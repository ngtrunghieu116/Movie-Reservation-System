package com.moviebooking.service.review;

import com.moviebooking.dto.req.CreateReviewRequest;
import com.moviebooking.dto.res.MovieRatingSummaryResponse;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.dto.res.ReviewResponse;
import com.moviebooking.model.User;
import com.moviebooking.model.enums.ReviewStatus;

public interface IReviewService {
    ReviewResponse submitReview(CreateReviewRequest request, User currentUser);
    ReviewResponse getMyReviewForMovie(Long movieId, User currentUser);
    PageResponse<ReviewResponse> getPublicMovieReviews(Long movieId, int page, int size);
    MovieRatingSummaryResponse getMovieRatingSummary(Long movieId);
    PageResponse<ReviewResponse> getAdminReviews(Long movieId, ReviewStatus status, String search, int page, int size);
    ReviewResponse updateReviewStatus(Long id, ReviewStatus status);
    void deleteReview(Long id);
}
