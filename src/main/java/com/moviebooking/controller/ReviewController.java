package com.moviebooking.controller;

import com.moviebooking.dto.req.CreateReviewRequest;
import com.moviebooking.dto.res.ReviewResponse;
import com.moviebooking.model.User;
import com.moviebooking.security.SecurityUtils;
import com.moviebooking.service.review.IReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final IReviewService reviewService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ReviewResponse> submitReview(@Valid @RequestBody CreateReviewRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(reviewService.submitReview(request, currentUser));
    }

    @GetMapping("/my-review/{movieId}")
    public ResponseEntity<ReviewResponse> getMyReview(@PathVariable Long movieId) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(reviewService.getMyReviewForMovie(movieId, currentUser));
    }
}
