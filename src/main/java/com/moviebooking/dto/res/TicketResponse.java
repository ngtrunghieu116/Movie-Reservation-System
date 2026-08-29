package com.moviebooking.dto.res;

import com.moviebooking.model.enums.TicketStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketResponse {
    private String ticketCode;
    private Long showtimeId;
    private String movieTitle;
    private String theaterName;
    private String roomName;
    private LocalDateTime startTime;
    private String seatName;
    private BigDecimal price;
    private TicketStatus status;
    private String qrCodeUrl;
    private LocalDateTime checkedInAt;
}
