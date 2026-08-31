package com.moviebooking.dto.res;

import com.moviebooking.model.enums.AgeRating;
import com.moviebooking.model.enums.PaymentMethod;
import com.moviebooking.model.enums.PaymentStatus;
import com.moviebooking.model.enums.ReservationStatus;
import com.moviebooking.model.enums.TicketStatus;
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
public class AdminBookingDetailResponse {
    // 1. Customer Info
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // 2. Booking Info
    private Long reservationId;
    private String bookingCode;
    private ReservationStatus bookingStatus;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    // 3. Movie Info
    private String movieTitle;
    private String posterPath;
    private AgeRating ageRating;
    private String language;
    private String subtitle;

    // 4. Showtime & Room Info
    private LocalDateTime showtimeStart;
    private LocalDateTime showtimeEnd;
    private String theaterName;
    private String roomName;
    private String roomType;

    // 5. Seats & Ticket Pricing Info
    private List<String> seatNames;
    private Integer ticketCount;
    private BigDecimal ticketSubtotal;

    // 6. F&B Info
    private List<FnbItemDetail> fnbItems;
    private BigDecimal fnbSubtotal;

    // 7. Payment Info
    private PaymentMethod paymentMethod;
    private String transactionRef;
    private String transactionNo;
    private String bankCode;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;

    // 8. Total Amount
    private BigDecimal totalAmount;

    // 9. Tickets with QR Code and Check-in Info
    private List<TicketAdminDetail> tickets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FnbItemDetail {
        private Long itemId;
        private Long productId;
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketAdminDetail {
        private String ticketCode;
        private String seatName;
        private BigDecimal price;
        private TicketStatus status;
        private String qrCodeUrl;
        private LocalDateTime checkedInAt;
    }
}
