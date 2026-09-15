package com.moviebooking.dto.res;

import java.math.BigDecimal;

public record MovieShareStatResponse(
        Long movieId,
        String movieTitle,
        BigDecimal revenue,
        Double revenuePercentage
) {}
