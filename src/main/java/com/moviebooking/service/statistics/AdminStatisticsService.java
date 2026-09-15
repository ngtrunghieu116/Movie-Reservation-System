package com.moviebooking.service.statistics;

import com.moviebooking.dto.res.DailyRevenueStatResponse;
import com.moviebooking.dto.res.DashboardOverviewResponse;
import com.moviebooking.dto.res.MovieShareStatResponse;
import com.moviebooking.dto.res.RoomPerformanceStatResponse;
import com.moviebooking.dto.res.TopMovieStatResponse;

import java.time.LocalDate;
import java.util.List;

public interface AdminStatisticsService {

    DashboardOverviewResponse getOverview(LocalDate date);

    List<DailyRevenueStatResponse> getDailyRevenueSeries(LocalDate from, LocalDate to);

    List<MovieShareStatResponse> getMovieRevenueShare(LocalDate from, LocalDate to);

    List<TopMovieStatResponse> getTopMovies(LocalDate from, LocalDate to, int limit);

    List<RoomPerformanceStatResponse> getRoomPerformance(LocalDate from, LocalDate to);
}
