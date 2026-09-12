package com.moviebooking.dto.res;

import java.math.BigDecimal;

public record DashboardOverviewResponse(
        BigDecimal todayRevenue,
        BigDecimal totalRevenue,
        Long todayTickets,
        Long totalTickets,
        Long todayConfirmedBookings,
        Long totalConfirmedBookings,
        Long todayNewUsers,
        Long totalUsers,
        BigDecimal currentMonthRevenue,
        Long todayShowtimes,
        Long activeMovies
) {}
