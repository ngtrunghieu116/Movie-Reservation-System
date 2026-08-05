package com.moviebooking.controller;

import com.moviebooking.dto.req.BatchGenerateSeatsRequest;
import com.moviebooking.dto.req.BatchUpdateSeatsRequest;
import com.moviebooking.dto.req.UpdateSeatRequest;
import com.moviebooking.dto.res.SeatResponse;
import com.moviebooking.service.seat.ISeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final ISeatService seatService;

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<SeatResponse>> getSeatsByRoomId(@PathVariable Long roomId) {
        return ResponseEntity.ok(seatService.getSeatsByRoomId(roomId));
    }

    @PostMapping("/room/{roomId}/generate")
    public ResponseEntity<List<SeatResponse>> generateSeatLayout(
            @PathVariable Long roomId,
            @Valid @RequestBody BatchGenerateSeatsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seatService.generateSeatLayout(roomId, request));
    }

    @PutMapping("/{seatId}")
    public ResponseEntity<SeatResponse> updateSeat(
            @PathVariable Long seatId,
            @RequestBody UpdateSeatRequest request) {
        return ResponseEntity.ok(seatService.updateSeat(seatId, request));
    }

    @PatchMapping("/batch-update")
    public ResponseEntity<List<SeatResponse>> batchUpdateSeats(
            @Valid @RequestBody BatchUpdateSeatsRequest request) {
        return ResponseEntity.ok(seatService.batchUpdateSeats(request));
    }
}
