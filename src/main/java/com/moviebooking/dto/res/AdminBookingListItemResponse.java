package com.moviebooking.dto.res;

import com.moviebooking.model.enums.PaymentStatus;
import com.moviebooking.model.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingListItemResponse {
    private Long reservationId;
    private String bookingCode;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String movieTitle;
    private LocalDateTime showtimeStart;
    private String roomName;
    private List<String> seatNames;
    private Integer ticketCount;
    private BigDecimal totalAmount;
    private ReservationStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
}
