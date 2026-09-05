package com.moviebooking.service.review;

import com.moviebooking.dto.req.CreateReviewRequest;
import com.moviebooking.dto.res.MovieRatingSummaryResponse;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.dto.res.ReviewResponse;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.model.Movie;
import com.moviebooking.model.Review;
import com.moviebooking.model.User;
import com.moviebooking.model.enums.ReservationStatus;
import com.moviebooking.model.enums.ReviewStatus;
import com.moviebooking.model.enums.TicketStatus;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ReviewRepository;
import com.moviebooking.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final TicketRepository ticketRepository;

    private static final List<TicketStatus> VALID_TICKET_STATUSES = Arrays.asList(
            TicketStatus.ISSUED,
            TicketStatus.CHECKED_IN,
            TicketStatus.VALID,
            TicketStatus.USED
    );

    @Override
    @Transactional
    public ReviewResponse submitReview(CreateReviewRequest request, User currentUser) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ phim với ID: " + request.getMovieId()));

        // Check verified purchase dynamically based on actual transactional booking data
        boolean isVerified = ticketRepository.existsVerifiedTicketForUserAndMovie(
                currentUser.getId(),
                request.getMovieId(),
                ReservationStatus.CONFIRMED,
                VALID_TICKET_STATUSES
        );

        Optional<Review> existingReviewOpt = reviewRepository.findByUserIdAndMovieId(currentUser.getId(), request.getMovieId());
        Review review;

        if (existingReviewOpt.isPresent()) {
            // Upsert: update existing review
            review = existingReviewOpt.get();
            review.setRating(request.getRating());
            review.setComment(request.getComment() != null ? request.getComment().trim() : null);
            review.setVerifiedPurchase(isVerified);
            // If it was deleted by admin or user, resubmission restores it to PUBLISHED
            if (review.getStatus() == ReviewStatus.DELETED) {
                review.setStatus(ReviewStatus.PUBLISHED);
            }
        } else {
            // Create new review
            review = Review.builder()
                    .user(currentUser)
                    .movie(movie)
                    .rating(request.getRating())
                    .comment(request.getComment() != null ? request.getComment().trim() : null)
                    .verifiedPurchase(isVerified)
                    .status(ReviewStatus.PUBLISHED)
                    .build();
        }

        Review saved = reviewRepository.save(review);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getMyReviewForMovie(Long movieId, User currentUser) {
        return reviewRepository.findByUserIdAndMovieId(currentUser.getId(), movieId)
                .filter(r -> r.getStatus() != ReviewStatus.DELETED)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getPublicMovieReviews(Long movieId, int page, int size) {
        if (!movieRepository.existsById(movieId)) {
            throw new ResourceNotFoundException("Không tìm thấy bộ phim với ID: " + movieId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findByMovieIdAndStatusOrderByCreatedAtDesc(movieId, ReviewStatus.PUBLISHED, pageable);

        List<ReviewResponse> content = reviewPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<ReviewResponse>builder()
                .content(content)
                .pageNo(reviewPage.getNumber())
                .pageSize(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MovieRatingSummaryResponse getMovieRatingSummary(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ phim với ID: " + movieId));

        List<Object[]> avgAndCount = reviewRepository.getAverageRatingAndCount(movieId, ReviewStatus.PUBLISHED);
        Double avgRating = 5.0;
        Long totalCount = 0L;

        if (avgAndCount != null && !avgAndCount.isEmpty()) {
            Object[] row = avgAndCount.get(0);
            if (row[1] != null) {
                totalCount = ((Number) row[1]).longValue();
            }
            if (totalCount > 0 && row[0] != null) {
                avgRating = ((Number) row[0]).doubleValue();
                avgRating = Math.round(avgRating * 10.0) / 10.0; // Làm tròn 1 chữ số thập phân
            }
        }

        long star1 = 0, star2 = 0, star3 = 0, star4 = 0, star5 = 0;
        List<Object[]> ratingCounts = reviewRepository.countReviewsByRatingGroup(movieId, ReviewStatus.PUBLISHED);
        if (ratingCounts != null) {
            for (Object[] r : ratingCounts) {
                Integer rating = ((Number) r[0]).intValue();
                Long count = ((Number) r[1]).longValue();
                if (rating == 1) star1 = count;
                else if (rating == 2) star2 = count;
                else if (rating == 3) star3 = count;
                else if (rating == 4) star4 = count;
                else if (rating == 5) star5 = count;
            }
        }

        return MovieRatingSummaryResponse.builder()
                .movieId(movie.getId())
                .movieTitle(movie.getTitle())
                .averageRating(avgRating)
                .totalReviews(totalCount)
                .star1Count(star1)
                .star2Count(star2)
                .star3Count(star3)
                .star4Count(star4)
                .star5Count(star5)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAdminReviews(Long movieId, ReviewStatus status, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<Review> reviewPage = reviewRepository.searchReviewsForAdmin(movieId, status, trimmedSearch, pageable);

        List<ReviewResponse> content = reviewPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<ReviewResponse>builder()
                .content(content)
                .pageNo(reviewPage.getNumber())
                .pageSize(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse updateReviewStatus(Long id, ReviewStatus status) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhận xét với ID: " + id));

        review.setStatus(status);
        Review updated = reviewRepository.save(review);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhận xét với ID: " + id));

        // Soft-delete as required
        review.setStatus(ReviewStatus.DELETED);
        reviewRepository.save(review);
    }

    private ReviewResponse mapToResponse(Review review) {
        String fullName = "";
        if (review.getUser() != null) {
            String firstName = review.getUser().getFirstName() != null ? review.getUser().getFirstName() : "";
            String lastName = review.getUser().getLastName() != null ? review.getUser().getLastName() : "";
            if (!lastName.isEmpty() && !firstName.isEmpty()) {
                fullName = (lastName + " " + firstName).trim();
            } else {
                fullName = (lastName + firstName).trim();
            }
            if (fullName.isEmpty()) {
                fullName = review.getUser().getEmail();
            }
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .movieId(review.getMovie() != null ? review.getMovie().getId() : null)
                .movieTitle(review.getMovie() != null ? review.getMovie().getTitle() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userFullName(fullName)
                .userEmail(review.getUser() != null ? review.getUser().getEmail() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.getVerifiedPurchase())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
