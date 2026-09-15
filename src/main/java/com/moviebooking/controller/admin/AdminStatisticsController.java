package com.moviebooking.controller.admin;

import com.moviebooking.dto.res.DailyRevenueStatResponse;
import com.moviebooking.dto.res.DashboardOverviewResponse;
import com.moviebooking.dto.res.MovieShareStatResponse;
import com.moviebooking.dto.res.RoomPerformanceStatResponse;
import com.moviebooking.dto.res.TopMovieStatResponse;
import com.moviebooking.service.statistics.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin Statistics Controller — feature/admin-dashboard-stats
 * Base path: /api/admin/statistics
 */
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService statisticsService;

    /**
     * GET /api/admin/statistics/overview?date=2026-09-12
     * 4 thẻ KPI tổng quan cho ngày được chọn.
     */
    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate target = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(statisticsService.getOverview(target));
    }

    /**
     * GET /api/admin/statistics/daily-revenue?from=2026-09-01&to=2026-09-30
     * Chuỗi dữ liệu theo ngày cho Line Chart.
     */
    @GetMapping("/daily-revenue")
    public ResponseEntity<List<DailyRevenueStatResponse>> getDailyRevenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate end   = (to   != null) ? to   : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(29);
        return ResponseEntity.ok(statisticsService.getDailyRevenueSeries(start, end));
    }

    /**
     * GET /api/admin/statistics/movie-share?from=...&to=...
     * Tỷ lệ đóng góp doanh thu theo phim (Donut Chart), tối đa 10 phim.
     */
    @GetMapping("/movie-share")
    public ResponseEntity<List<MovieShareStatResponse>> getMovieShare(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate end   = (to   != null) ? to   : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(29);
        return ResponseEntity.ok(statisticsService.getMovieRevenueShare(start, end));
    }

    /**
     * GET /api/admin/statistics/top-movies?from=...&to=...&limit=10
     * Bảng Top phim theo doanh thu.
     */
    @GetMapping("/top-movies")
    public ResponseEntity<List<TopMovieStatResponse>> getTopMovies(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {

        LocalDate end   = (to   != null) ? to   : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(29);
        return ResponseEntity.ok(statisticsService.getTopMovies(start, end, limit));
    }

    /**
     * GET /api/admin/statistics/room-performance?from=...&to=...
     * Bảng hiệu suất phòng chiếu.
     */
    @GetMapping("/room-performance")
    public ResponseEntity<List<RoomPerformanceStatResponse>> getRoomPerformance(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate end   = (to   != null) ? to   : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(29);
        return ResponseEntity.ok(statisticsService.getRoomPerformance(start, end));
    }
}
