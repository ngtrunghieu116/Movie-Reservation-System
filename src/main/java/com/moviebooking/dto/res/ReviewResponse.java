package com.moviebooking.dto.res;

import com.moviebooking.model.enums.ReviewStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Integer rating;
    private String comment;
    private Boolean verifiedPurchase;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
