package com.moviebooking.controller;

import com.moviebooking.dto.res.MovieRatingSummaryResponse;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.dto.res.ReviewResponse;
import com.moviebooking.service.review.IReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies/{movieId}")
@RequiredArgsConstructor
public class PublicReviewController {

    private final IReviewService reviewService;

    @GetMapping("/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getMovieReviews(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getPublicMovieReviews(movieId, page, size));
    }

    @GetMapping("/rating")
    public ResponseEntity<MovieRatingSummaryResponse> getMovieRatingSummary(@PathVariable Long movieId) {
        return ResponseEntity.ok(reviewService.getMovieRatingSummary(movieId));
    }
}
