package com.moviebooking.dto.res;

import java.math.BigDecimal;
import java.util.List;

public record TopMovieStatResponse(
        Long movieId,
        String movieTitle,
        String posterPath,
        List<String> genres,
        Long ticketsSold,
        Long bookingCount,
        BigDecimal revenue,
        Double revenuePercentage
) {}
