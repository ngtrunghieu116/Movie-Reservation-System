package com.moviebooking.service.statistics;

import com.moviebooking.dto.res.DailyRevenueStatResponse;
import com.moviebooking.dto.res.DashboardOverviewResponse;
import com.moviebooking.dto.res.MovieShareStatResponse;
import com.moviebooking.dto.res.RoomPerformanceStatResponse;
import com.moviebooking.dto.res.TopMovieStatResponse;
import com.moviebooking.repository.ReservationRepository;
import com.moviebooking.repository.TicketRepository;
import com.moviebooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    // ─── Overview ─────────────────────────────────────────────
    @Override
    public DashboardOverviewResponse getOverview(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd   = date.atTime(LocalTime.MAX);

        LocalDateTime monthStart = date.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd   = date.withDayOfMonth(date.lengthOfMonth()).atTime(LocalTime.MAX);

        BigDecimal todayRevenue   = nullSafe(reservationRepository.sumRevenueBetween(dayStart, dayEnd));
        BigDecimal totalRevenue   = nullSafe(reservationRepository.sumTotalRevenue());
        Long todayTickets         = nullSafe(ticketRepository.countTicketsSoldBetween(dayStart, dayEnd));
        Long totalTickets         = nullSafe(ticketRepository.countTotalTicketsSold());
        Long todayBookings        = nullSafe(reservationRepository.countConfirmedBetween(dayStart, dayEnd));
        Long totalBookings        = nullSafe(reservationRepository.countTotalConfirmed());
        Long todayNewUsers        = nullSafe(userRepository.countNewUsersBetween(dayStart, dayEnd));
        Long totalUsers           = nullSafe(userRepository.countTotalUsers());
        BigDecimal monthRevenue   = nullSafe(reservationRepository.sumRevenueBetween(monthStart, monthEnd));
        Long todayShowtimes       = nullSafe(reservationRepository.countShowtimesBetween(dayStart, dayEnd));
        Long activeMovies         = nullSafe(reservationRepository.countActiveMoviesBetween(monthStart, monthEnd));

        return new DashboardOverviewResponse(
                todayRevenue, totalRevenue,
                todayTickets, totalTickets,
                todayBookings, totalBookings,
                todayNewUsers, totalUsers,
                monthRevenue, todayShowtimes, activeMovies
        );
    }

    // ─── Daily Revenue Series ─────────────────────────────────
    @Override
    public List<DailyRevenueStatResponse> getDailyRevenueSeries(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(LocalTime.MAX);

        // Fetch raw revenue data
        List<Object[]> revenueRows = reservationRepository.findDailyRevenueBetween(start, end);
        // Fetch raw ticket data
        List<Object[]> ticketRows  = ticketRepository.countDailyTicketsBetween(start, end);

        // Build lookup maps (date-string → value)
        Map<String, BigDecimal> revenueByDate  = new HashMap<>();
        Map<String, Long>       bookingByDate  = new HashMap<>();
        for (Object[] row : revenueRows) {
            String dateKey = row[0].toString().substring(0, 10); // yyyy-MM-dd
            revenueByDate.put(dateKey, toBigDecimal(row[1]));
            bookingByDate.put(dateKey, toLong(row[2]));
        }
        Map<String, Long> ticketByDate = new HashMap<>();
        for (Object[] row : ticketRows) {
            String dateKey = row[0].toString().substring(0, 10);
            ticketByDate.put(dateKey, toLong(row[1]));
        }

        // Zero-fill all days in range
        List<DailyRevenueStatResponse> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String key = d.toString();
            result.add(new DailyRevenueStatResponse(
                    d,
                    revenueByDate.getOrDefault(key, BigDecimal.ZERO),
                    ticketByDate.getOrDefault(key, 0L),
                    bookingByDate.getOrDefault(key, 0L)
            ));
        }
        return result;
    }

    // ─── Movie Revenue Share (Donut) ──────────────────────────
    @Override
    public List<MovieShareStatResponse> getMovieRevenueShare(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(LocalTime.MAX);

        List<Object[]> rows = reservationRepository.findMovieRevenueBetween(start, end, 10);

        // Calculate total to compute percentage
        BigDecimal total = rows.stream()
                .map(r -> toBigDecimal(r[3]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MovieShareStatResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            BigDecimal rev = toBigDecimal(row[3]);
            double pct = total.compareTo(BigDecimal.ZERO) == 0 ? 0
                    : rev.multiply(BigDecimal.valueOf(100))
                         .divide(total, 2, RoundingMode.HALF_UP)
                         .doubleValue();
            result.add(new MovieShareStatResponse(
                    toLong(row[0]),
                    (String) row[1],
                    rev,
                    pct
            ));
        }
        return result;
    }

    // ─── Top Movies Table ─────────────────────────────────────
    @Override
    public List<TopMovieStatResponse> getTopMovies(LocalDate from, LocalDate to, int limit) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(LocalTime.MAX);

        List<Object[]> revenueRows = reservationRepository.findMovieRevenueBetween(start, end, limit);
        List<Object[]> ticketRows  = ticketRepository.countTicketsByMovieBetween(start, end);

        // Build ticket lookup
        Map<Long, Long> ticketByMovie = new HashMap<>();
        for (Object[] tr : ticketRows) {
            ticketByMovie.put(toLong(tr[0]), toLong(tr[1]));
        }

        BigDecimal total = revenueRows.stream()
                .map(r -> toBigDecimal(r[3]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TopMovieStatResponse> result = new ArrayList<>();
        for (Object[] row : revenueRows) {
            Long movieId  = toLong(row[0]);
            String title  = (String) row[1];
            String poster = (String) row[2];
            BigDecimal rev  = toBigDecimal(row[3]);
            Long bookings   = toLong(row[4]);
            Long tickets    = ticketByMovie.getOrDefault(movieId, 0L);
            double pct = total.compareTo(BigDecimal.ZERO) == 0 ? 0
                    : rev.multiply(BigDecimal.valueOf(100))
                         .divide(total, 2, RoundingMode.HALF_UP)
                         .doubleValue();

            result.add(new TopMovieStatResponse(
                    movieId, title, poster,
                    List.of(), // genres not needed for now
                    tickets, bookings, rev, pct
            ));
        }
        return result;
    }

    // ─── Room Performance Table ───────────────────────────────
    @Override
    public List<RoomPerformanceStatResponse> getRoomPerformance(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(LocalTime.MAX);

        List<Object[]> revenueRows = reservationRepository.findRoomPerformanceBetween(start, end);
        List<Object[]> ticketRows  = ticketRepository.countTicketsByRoomBetween(start, end);

        Map<Long, Long> ticketByRoom = new HashMap<>();
        for (Object[] tr : ticketRows) {
            ticketByRoom.put(toLong(tr[0]), toLong(tr[1]));
        }

        List<RoomPerformanceStatResponse> result = new ArrayList<>();
        for (Object[] row : revenueRows) {
            Long roomId       = toLong(row[0]);
            String roomName   = (String) row[1];
            String roomType   = row[2] != null ? row[2].toString() : "";
            Long showtimes    = toLong(row[3]);
            BigDecimal rev    = toBigDecimal(row[4]);
            Long tickets      = ticketByRoom.getOrDefault(roomId, 0L);

            result.add(new RoomPerformanceStatResponse(
                    roomId, roomName, roomType, showtimes, tickets, rev
            ));
        }
        return result;
    }

    // ─── Helpers ──────────────────────────────────────────────
    private BigDecimal nullSafe(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private Long nullSafe(Long v)             { return v != null ? v : 0L; }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        return new BigDecimal(v.toString());
    }

    private Long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }
}
