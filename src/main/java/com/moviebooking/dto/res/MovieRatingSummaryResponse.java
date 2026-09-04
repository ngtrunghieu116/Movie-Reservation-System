package com.moviebooking.dto.res;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieRatingSummaryResponse {
    private Long movieId;
    private String movieTitle;
    private Double averageRating;
    private Long totalReviews;
    private Long star1Count;
    private Long star2Count;
    private Long star3Count;
    private Long star4Count;
    private Long star5Count;
}
