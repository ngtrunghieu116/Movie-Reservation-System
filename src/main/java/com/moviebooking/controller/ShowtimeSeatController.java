package com.moviebooking.controller;

import com.moviebooking.dto.req.HoldSeatsRequest;
import com.moviebooking.dto.req.ReleaseSeatsRequest;
import com.moviebooking.dto.res.HoldSeatsResponse;
import com.moviebooking.model.User;
import com.moviebooking.security.SecurityUtils;
import com.moviebooking.service.seat.ShowtimeSeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/showtime-seats")
@RequiredArgsConstructor
public class ShowtimeSeatController {

    private final ShowtimeSeatService showtimeSeatService;
    private final SecurityUtils securityUtils;

    @PostMapping("/hold")
    public ResponseEntity<HoldSeatsResponse> holdSeats(@Valid @RequestBody HoldSeatsRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        HoldSeatsResponse response = showtimeSeatService.holdSeats(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/release")
    public ResponseEntity<Map<String, String>> releaseSeats(@Valid @RequestBody ReleaseSeatsRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        showtimeSeatService.releaseSeats(request, currentUser);
        return ResponseEntity.ok(Map.of("message", "Giải phóng ghế thành công"));
    }
}
