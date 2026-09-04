package com.moviebooking.controller.admin;

import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.dto.res.ReviewResponse;
import com.moviebooking.model.enums.ReviewStatus;
import com.moviebooking.service.review.IReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final IReviewService reviewService;

    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getAdminReviews(movieId, status, search, page, size));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<ReviewResponse> publishReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.updateReviewStatus(id, ReviewStatus.PUBLISHED));
    }

    @PatchMapping("/{id}/hide")
    public ResponseEntity<ReviewResponse> hideReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.updateReviewStatus(id, ReviewStatus.HIDDEN));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
