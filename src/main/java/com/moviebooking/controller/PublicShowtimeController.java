package com.moviebooking.controller;

import com.moviebooking.dto.res.PublicShowtimeResponse;
import com.moviebooking.service.showtime.IShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class PublicShowtimeController {

    private final IShowtimeService showtimeService;

    @GetMapping
    public ResponseEntity<Page<PublicShowtimeResponse>> searchShowtimes(
            @RequestParam(required = false) Long theaterId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(showtimeService.searchPublicShowtimes(theaterId, roomId, movieId, fromDate, toDate, page, size));
    }
}
