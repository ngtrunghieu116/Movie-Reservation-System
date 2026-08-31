package com.moviebooking.booking;

import com.moviebooking.dto.res.PaymentStatusDetailResponse;
import com.moviebooking.dto.res.UserBookingHistoryItemResponse;
import com.moviebooking.exception.*;
import com.moviebooking.model.*;
import com.moviebooking.model.enums.*;
import com.moviebooking.repository.*;
import com.moviebooking.service.booking.BookingService;
import com.moviebooking.service.booking.TicketService;
import com.moviebooking.service.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class Phase6BookingHistoryAndPaymentStatusTest {

    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private TheaterRepository theaterRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private ReservedSeatRepository reservedSeatRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private TicketService ticketService;

    private User userA;
    private User userB;
    private Movie movie;
    private Showtime showtime;
    private Seat seat1;
    private Seat seat2;
    private Product comboPopcorn;

    private Reservation confirmedReservationUserA;
    private Reservation pendingReservationUserA;
    private Reservation cancelledReservationUserA;
    private Reservation confirmedReservationUserB;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder()
                .email("usera@example.com")
                .password("password")
                .firstName("Nguyen")
                .lastName("Van A")
                .phone("0901111111")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build());

        userB = userRepository.save(User.builder()
                .email("userb@example.com")
                .password("password")
                .firstName("Tran")
                .lastName("Thi B")
                .phone("0902222222")
                .dateOfBirth(LocalDate.of(1995, 2, 2))
                .gender(Gender.FEMALE)
                .role(Role.USER)
                .build());

        movie = movieRepository.save(Movie.builder()
                .title("ZOOTOPIA: PHI VU DONG TROI 2")
                .description("Phim hoat hinh Zootopia 2")
                .director("Byron Howard")
                .actors("Ginnifer Goodwin, Jason Bateman")
                .duration(108)
                .releaseDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .posterPath("https://image.tmdb.org/t/p/w500/poster.jpg")
                .ageRating(AgeRating.P)
                .language("Tieng Viet")
                .subtitle("Long tieng")
                .status(MovieStatus.NOW_SHOWING)
                .build());

        Theater theater = theaterRepository.save(Theater.builder()
                .name("Trung Tam Chieu Phim Quoc Gia")
                .address("87 Lang Ha, Ba Dinh, Ha Noi")
                .phone("02435141791")
                .city("Hanoi")
                .district("Ba Dinh")
                .isActive(true)
                .build());

        Room room = roomRepository.save(Room.builder()
                .name("Phòng chiếu 4")
                .roomType(RoomType.TWO_D)
                .isActive(true)
                .theater(theater)
                .build());

        showtime = showtimeRepository.save(Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(LocalDateTime.now().plusHours(3))
                .endTime(LocalDateTime.now().plusHours(5))
                .priceStandard(new BigDecimal("85000.00"))
                .priceVip(new BigDecimal("95000.00"))
                .priceCouple(new BigDecimal("120000.00"))
                .isActive(true)
                .build());

        seat1 = seatRepository.save(Seat.builder()
                .room(room)
                .rowName("J")
                .seatNumber(9)
                .seatType(SeatType.STANDARD)
                .build());

        seat2 = seatRepository.save(Seat.builder()
                .room(room)
                .rowName("J")
                .seatNumber(10)
                .seatType(SeatType.STANDARD)
                .build());

        comboPopcorn = productRepository.save(Product.builder()
                .name("Combo 1 Bap + 1 Nuoc")
                .price(new BigDecimal("65000.00"))
                .category(ProductCategory.COMBO)
                .isActive(true)
                .availableQuantity(100)
                .displayOrder(0)
                .build());

        // 1. Confirmed Reservation for User A
        confirmedReservationUserA = reservationRepository.save(Reservation.builder()
                .bookingCode("BK-USERA-CONFIRMED-001")
                .user(userA)
                .showtime(showtime)
                .totalPrice(new BigDecimal("235000.00")) // 2 seats * 85k + 65k combo = 235k
                .status(ReservationStatus.CONFIRMED)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build());

        reservedSeatRepository.save(ReservedSeat.builder()
                .reservation(confirmedReservationUserA)
                .seat(seat1)
                .price(new BigDecimal("85000.00"))
                .build());
        reservedSeatRepository.save(ReservedSeat.builder()
                .reservation(confirmedReservationUserA)
                .seat(seat2)
                .price(new BigDecimal("85000.00"))
                .build());

        orderItemRepository.save(OrderItem.builder()
                .reservation(confirmedReservationUserA)
                .product(comboPopcorn)
                .quantity(1)
                .unitPrice(new BigDecimal("65000.00"))
                .subtotal(new BigDecimal("65000.00"))
                .build());

        paymentRepository.save(Payment.builder()
                .reservation(confirmedReservationUserA)
                .transactionRef("TXN-USERA-001")
                .transactionNo("VNPAY-999001")
                .paymentMethod(PaymentMethod.VNPAY)
                .bankCode("NCB")
                .amount(new BigDecimal("235000.00"))
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .build());

        // Generate tickets for confirmed reservation
        ticketService.generateTicketsForReservation(confirmedReservationUserA);

        // 2. Pending Reservation for User A
        pendingReservationUserA = reservationRepository.save(Reservation.builder()
                .bookingCode("BK-USERA-PENDING-002")
                .user(userA)
                .showtime(showtime)
                .totalPrice(new BigDecimal("85000.00"))
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build());

        paymentRepository.save(Payment.builder()
                .reservation(pendingReservationUserA)
                .transactionRef("TXN-USERA-PENDING-002")
                .paymentMethod(PaymentMethod.VNPAY)
                .amount(new BigDecimal("85000.00"))
                .status(PaymentStatus.PENDING)
                .build());

        // 3. Cancelled Reservation for User A
        cancelledReservationUserA = reservationRepository.save(Reservation.builder()
                .bookingCode("BK-USERA-CANCELLED-003")
                .user(userA)
                .showtime(showtime)
                .totalPrice(new BigDecimal("85000.00"))
                .status(ReservationStatus.CANCELLED)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build());

        paymentRepository.save(Payment.builder()
                .reservation(cancelledReservationUserA)
                .transactionRef("TXN-USERA-FAILED-003")
                .paymentMethod(PaymentMethod.VNPAY)
                .amount(new BigDecimal("85000.00"))
                .status(PaymentStatus.FAILED)
                .build());

        // 4. Confirmed Reservation for User B
        confirmedReservationUserB = reservationRepository.save(Reservation.builder()
                .bookingCode("BK-USERB-CONFIRMED-999")
                .user(userB)
                .showtime(showtime)
                .totalPrice(new BigDecimal("85000.00"))
                .status(ReservationStatus.CONFIRMED)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build());

        paymentRepository.save(Payment.builder()
                .reservation(confirmedReservationUserB)
                .transactionRef("TXN-USERB-001")
                .transactionNo("VNPAY-999002")
                .paymentMethod(PaymentMethod.VNPAY)
                .bankCode("NCB")
                .amount(new BigDecimal("85000.00"))
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("TC-01: User có booking CONFIRMED + payment COMPLETED xuất hiện trong my-history")
    void testConfirmedBookingAppearsInHistory() {
        List<UserBookingHistoryItemResponse> history = bookingService.getMyBookingHistory(userA);

        assertThat(history).isNotEmpty();
        assertThat(history).hasSize(1); // Only 1 successful booking
        UserBookingHistoryItemResponse item = history.get(0);
        assertThat(item.getOrderId()).isEqualTo("BK-USERA-CONFIRMED-001");
        assertThat(item.getMovieTitle()).isEqualTo("ZOOTOPIA: PHI VU DONG TROI 2");
        assertThat(item.getTransactionType()).isEqualTo("Mua online");
        assertThat(item.getTicketCount()).isEqualTo(2);
        assertThat(item.getTotalAmount()).isEqualByComparingTo(new BigDecimal("235000.00"));
    }

    @Test
    @DisplayName("TC-02: Booking PENDING không xuất hiện trong my-history")
    void testPendingBookingDoesNotAppearInHistory() {
        List<UserBookingHistoryItemResponse> history = bookingService.getMyBookingHistory(userA);
        boolean containsPending = history.stream()
                .anyMatch(h -> h.getOrderId().equals(pendingReservationUserA.getBookingCode()));
        assertThat(containsPending).isFalse();
    }

    @Test
    @DisplayName("TC-03: Booking CANCELLED không xuất hiện trong my-history")
    void testCancelledBookingDoesNotAppearInHistory() {
        List<UserBookingHistoryItemResponse> history = bookingService.getMyBookingHistory(userA);
        boolean containsCancelled = history.stream()
                .anyMatch(h -> h.getOrderId().equals(cancelledReservationUserA.getBookingCode()));
        assertThat(containsCancelled).isFalse();
    }

    @Test
    @DisplayName("TC-04: User A truy cập booking User B bị từ chối 403 (SeatHoldOwnershipException)")
    void testUserCannotAccessOtherUserBooking() {
        assertThrows(SeatHoldOwnershipException.class, () ->
                paymentService.getPaymentStatusDetail("BK-USERB-CONFIRMED-999", userA)
        );
    }

    @Test
    @DisplayName("TC-05: orderId bằng bookingCode lấy đúng booking và thông tin vé")
    void testGetPaymentStatusByBookingCode() {
        PaymentStatusDetailResponse detail = paymentService.getPaymentStatusDetail("BK-USERA-CONFIRMED-001", userA);

        assertThat(detail).isNotNull();
        assertThat(detail.getBookingCode()).isEqualTo("BK-USERA-CONFIRMED-001");
        assertThat(detail.getCustomerName()).isEqualTo("Nguyen Van A");
        assertThat(detail.getMovieTitle()).isEqualTo("ZOOTOPIA: PHI VU DONG TROI 2");
        assertThat(detail.getRoomName()).isEqualTo("Phòng chiếu 4");
        assertThat(detail.getRoomType()).isEqualTo("2D");
        assertThat(detail.getSeatNames()).containsExactlyInAnyOrder("J9", "J10");
        assertThat(detail.getTicketCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-06: orderId không tồn tại trả về 404 (ResourceNotFoundException)")
    void testNonExistentOrderIdThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.getPaymentStatusDetail("NON-EXISTENT-CODE-999", userA)
        );
    }

    @Test
    @DisplayName("TC-07: Payment COMPLETED trả về đầy đủ Ticket + QR + F&B")
    void testPaymentCompletedReturnsTicketsAndFnb() {
        PaymentStatusDetailResponse detail = paymentService.getPaymentStatusDetail("BK-USERA-CONFIRMED-001", userA);

        assertThat(detail.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(detail.getTransactionNo()).isEqualTo("VNPAY-999001");
        assertThat(detail.getBankCode()).isEqualTo("NCB");
        assertThat(detail.getTotalAmount()).isEqualByComparingTo(new BigDecimal("235000.00"));

        // Verify Tickets and QR Code
        assertThat(detail.getTickets()).hasSize(2);
        for (var ticket : detail.getTickets()) {
            assertThat(ticket.getTicketCode()).isNotBlank();
            assertThat(ticket.getQrCodeUrl()).startsWith("data:image/png;base64,");
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ISSUED);
        }

        // Verify F&B Items
        assertThat(detail.getFnbItems()).hasSize(1);
        assertThat(detail.getFnbItems().get(0).getProductName()).isEqualTo("Combo 1 Bap + 1 Nuoc");
        assertThat(detail.getFnbItems().get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-08: Ticket ISSUED có thể check-in thành công")
    void testTicketIssuedCanCheckIn() {
        List<Ticket> tickets = ticketRepository.findByReservationId(confirmedReservationUserA.getId());
        assertThat(tickets).isNotEmpty();

        Ticket ticket = tickets.get(0);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ISSUED);

        ticketService.checkInTicket(ticket.getTicketCode());

        Ticket checkedInTicket = ticketRepository.findByTicketCode(ticket.getTicketCode()).orElseThrow();
        assertThat(checkedInTicket.getStatus()).isEqualTo(TicketStatus.CHECKED_IN);
        assertThat(checkedInTicket.getCheckedInAt()).isNotNull();
    }

    @Test
    @DisplayName("TC-09: Ticket CHECKED_IN không thể check-in lần 2 (TicketAlreadyCheckedInException)")
    void testTicketCheckedInCannotCheckInTwice() {
        List<Ticket> tickets = ticketRepository.findByReservationId(confirmedReservationUserA.getId());
        Ticket ticket = tickets.get(0);

        ticketService.checkInTicket(ticket.getTicketCode());

        assertThrows(TicketAlreadyCheckedInException.class, () ->
                ticketService.checkInTicket(ticket.getTicketCode())
        );
    }

    @Test
    @DisplayName("TC-10: Ticket CANCELLED không thể check-in (TicketValidationException)")
    void testCancelledTicketCannotCheckIn() {
        List<Ticket> tickets = ticketRepository.findByReservationId(confirmedReservationUserA.getId());
        Ticket ticket = tickets.get(0);
        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);

        assertThrows(TicketValidationException.class, () ->
                ticketService.checkInTicket(ticket.getTicketCode())
        );
    }
}
