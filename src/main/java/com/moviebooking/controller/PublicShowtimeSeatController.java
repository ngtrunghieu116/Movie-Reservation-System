package com.moviebooking.controller;

import com.moviebooking.dto.res.PublicShowtimeSeatResponse;
import com.moviebooking.service.seat.ShowtimeSeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/showtimes")
@RequiredArgsConstructor
public class PublicShowtimeSeatController {

    private final ShowtimeSeatService showtimeSeatService;

    @GetMapping("/{showtimeId}/seats")
    public ResponseEntity<List<PublicShowtimeSeatResponse>> getSeatMap(@PathVariable Long showtimeId) {
        List<PublicShowtimeSeatResponse> seatMap = showtimeSeatService.getPublicSeatMap(showtimeId);
        return ResponseEntity.ok(seatMap);
    }
}
