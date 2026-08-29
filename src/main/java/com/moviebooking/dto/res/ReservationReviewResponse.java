package com.moviebooking.dto.res;

import com.moviebooking.model.enums.ReservationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationReviewResponse {
    private Long reservationId;
    private String bookingCode;
    private String movieTitle;
    private LocalDateTime showtimeStart;
    private List<ReservedSeatDTO> ticketSeats;
    private BigDecimal ticketSubtotal;
    private List<OrderItemResponse> items;
    private BigDecimal fnbSubtotal;
    private BigDecimal totalAmount;
    private ReservationStatus status;
    private LocalDateTime expiresAt;
}
