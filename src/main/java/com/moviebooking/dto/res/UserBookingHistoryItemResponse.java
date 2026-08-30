package com.moviebooking.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookingHistoryItemResponse {
    private Long reservationId;
    private String orderId; // bookingCode e.g. "BK-178812..."
    private LocalDateTime transactionDate;
    private String movieTitle;
    private String transactionType; // "Mua online"
    private Integer ticketCount;
    private BigDecimal totalAmount;
}
