package com.moviebooking.controller.admin;

import com.moviebooking.dto.res.AdminBookingDetailResponse;
import com.moviebooking.dto.res.AdminBookingListItemResponse;
import com.moviebooking.model.enums.PaymentStatus;
import com.moviebooking.model.enums.ReservationStatus;
import com.moviebooking.service.booking.AdminBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    @GetMapping
    public ResponseEntity<Page<AdminBookingListItemResponse>> getBookings(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ReservationStatus bookingStatus,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showtimeDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = (sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1]))
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

        Page<AdminBookingListItemResponse> response = adminBookingService.getBookings(
                search,
                bookingStatus,
                paymentStatus,
                showtimeDate,
                pageable
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminBookingDetailResponse> getBookingDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminBookingService.getBookingDetail(id));
    }
}
