package com.moviebooking.dto.res;

import com.moviebooking.model.enums.PaymentMethod;
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
public class PaymentStatusDetailResponse {
    // 1. Booking / Reservation
    private Long reservationId;
    private String bookingCode;
    private String orderId; // bookingCode
    private ReservationStatus reservationStatus;
    private LocalDateTime transactionDate;

    // 2. User / Customer
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // 3. Movie
    private String movieTitle;
    private String posterPath;
    private String ageRating;
    private String language;
    private String subtitle;
    private String roomType; // e.g. "2D", "3D"

    // 4. Showtime
    private LocalDateTime showtimeStart;
    private LocalDateTime showtimeEnd;
    private String theaterName;
    private String roomName;

    // 5. Seats & Tickets
    private Integer ticketCount;
    private List<String> seatNames; // e.g. ["J9", "J10"]
    private List<TicketResponse> tickets; // ticketCode, seatName, price, status, qrCodeUrl, checkedInAt

    // 6. F&B
    private List<OrderItemResponse> fnbItems;
    private BigDecimal fnbSubtotal;

    // 7. Payment
    private PaymentMethod paymentMethod;
    private String transactionNo;
    private String transactionRef;
    private String bankCode;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;

    // 8. Total
    private BigDecimal ticketSubtotal;
    private BigDecimal totalAmount;
}
