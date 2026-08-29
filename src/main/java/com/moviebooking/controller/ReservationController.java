package com.moviebooking.controller;

import com.moviebooking.dto.req.AddComboRequest;
import com.moviebooking.dto.req.CreateReservationRequest;
import com.moviebooking.dto.req.UpdateComboQuantityRequest;
import com.moviebooking.dto.res.ReservationReviewResponse;
import com.moviebooking.model.User;
import com.moviebooking.security.SecurityUtils;
import com.moviebooking.service.booking.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final BookingService bookingService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ReservationReviewResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(bookingService.createReservation(request, currentUser));
    }

    @PostMapping("/{reservationId}/items")
    public ResponseEntity<ReservationReviewResponse> addComboToReservation(
            @PathVariable Long reservationId,
            @Valid @RequestBody AddComboRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(bookingService.addComboToReservation(reservationId, request, currentUser));
    }

    @PutMapping("/{reservationId}/items/{itemId}")
    public ResponseEntity<ReservationReviewResponse> updateComboQuantity(
            @PathVariable Long reservationId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateComboQuantityRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(bookingService.updateComboQuantity(reservationId, itemId, request, currentUser));
    }

    @DeleteMapping("/{reservationId}/items/{itemId}")
    public ResponseEntity<ReservationReviewResponse> removeComboFromReservation(
            @PathVariable Long reservationId,
            @PathVariable Long itemId) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(bookingService.removeComboFromReservation(reservationId, itemId, currentUser));
    }

    @GetMapping("/{reservationId}/review")
    public ResponseEntity<ReservationReviewResponse> reviewReservation(
            @PathVariable Long reservationId) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(bookingService.reviewReservation(reservationId, currentUser));
    }

    @GetMapping("/history")
    public ResponseEntity<java.util.List<com.moviebooking.dto.res.ReservationHistoryResponse>> getBookingHistory() {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(bookingService.getBookingHistory(currentUser));
    }
}
