package com.moviebooking.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueStatResponse(
        LocalDate date,
        BigDecimal revenue,
        Long ticketCount,
        Long bookingCount
) {}
