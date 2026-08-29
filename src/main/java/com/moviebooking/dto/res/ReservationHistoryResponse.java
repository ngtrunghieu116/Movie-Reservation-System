package com.moviebooking.dto.res;

import com.moviebooking.model.enums.PaymentStatus;
import com.moviebooking.model.enums.ReservationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReservationHistoryResponse {
    private Long reservationId;
    private String bookingCode;
    private String movieTitle;
    private String theaterName;
    private String roomName;
    private LocalDateTime showtimeStart;
    private ReservationStatus status;
    private PaymentStatus paymentStatus;
    private BigDecimal ticketSubtotal;
    private BigDecimal fnbSubtotal;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    
    private List<ReservedSeatDTO> ticketSeats;
    private List<OrderItemResponse> fnbItems;
    private List<TicketResponse> tickets;
}
