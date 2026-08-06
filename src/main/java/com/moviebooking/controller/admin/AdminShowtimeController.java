package com.moviebooking.controller.admin;

import com.moviebooking.dto.req.ShowtimeRequest;
import com.moviebooking.dto.res.AdminShowtimeResponse;
import com.moviebooking.service.showtime.IShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/showtimes")
@RequiredArgsConstructor
public class AdminShowtimeController {

    private final IShowtimeService showtimeService;

    @PostMapping
    public ResponseEntity<AdminShowtimeResponse> createShowtime(@Valid @RequestBody ShowtimeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(showtimeService.createShowtime(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminShowtimeResponse> updateShowtime(
            @PathVariable Long id,
            @Valid @RequestBody ShowtimeRequest request) {
        return ResponseEntity.ok(showtimeService.updateShowtime(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteShowtime(@PathVariable Long id) {
        showtimeService.deleteShowtime(id);
        return ResponseEntity.ok(Map.of("message", "Xóa suất chiếu thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminShowtimeResponse> getShowtimeById(@PathVariable Long id) {
        return ResponseEntity.ok(showtimeService.getShowtimeById(id));
    }

    @GetMapping
    public ResponseEntity<Page<AdminShowtimeResponse>> searchShowtimes(
            @RequestParam(required = false) Long theaterId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity
                .ok(showtimeService.searchShowtimes(theaterId, roomId, movieId, fromDate, toDate, page, size));
    }
}
