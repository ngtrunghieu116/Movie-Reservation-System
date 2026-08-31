package com.moviebooking.booking;

import com.moviebooking.dto.res.AdminBookingDetailResponse;
import com.moviebooking.dto.res.AdminBookingListItemResponse;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.model.*;
import com.moviebooking.model.enums.*;
import com.moviebooking.repository.*;
import com.moviebooking.service.booking.AdminBookingService;
import com.moviebooking.service.booking.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class AdminBookingManagementTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

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
    private AdminBookingService adminBookingService;
    @Autowired
    private TicketService ticketService;

    private User userA;
    private User userB;
    private Movie movie1;
    private Movie movie2;
    private Showtime showtimeToday;
    private Showtime showtimeTomorrow;
    private Seat seatA1;
    private Seat seatA2;
    private Product comboPopcorn;

    private Reservation resConfirmedA;
    private Reservation resPendingA;
    private Reservation resCancelledA;
    private Reservation resConfirmedB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

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
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .gender(Gender.FEMALE)
                .role(Role.USER)
                .build());

        movie1 = movieRepository.save(Movie.builder()
                .title("ZOOTOPIA: PHI VU DONG TROI 2")
                .description("Phim hoat hinh Zootopia 2")
                .director("Byron Howard")
                .actors("Ginnifer Goodwin, Jason Bateman")
                .duration(108)
                .releaseDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .posterPath("https://image.tmdb.org/t/p/w500/poster1.jpg")
                .ageRating(AgeRating.P)
                .language("Tieng Viet")
                .subtitle("Long tieng")
                .status(MovieStatus.NOW_SHOWING)
                .build());

        movie2 = movieRepository.save(Movie.builder()
                .title("AVATAR 3: FIRE AND ASH")
                .description("Phim khoa hoc vien tuong Avatar 3")
                .director("James Cameron")
                .actors("Sam Worthington, Zoe Saldana")
                .duration(190)
                .releaseDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .posterPath("https://image.tmdb.org/t/p/w500/poster2.jpg")
                .ageRating(AgeRating.T13)
                .language("Tieng Anh")
                .subtitle("Phu de")
                .status(MovieStatus.NOW_SHOWING)
                .build());

        Theater theater = theaterRepository.save(Theater.builder()
                .name("Trung Tam Chieu Phim Quoc Gia")
                .address("87 Lang Ha, Ba Dinh, Ha Noi")
                .city("Ha Noi")
                .district("Ba Dinh")
                .phone("02435141791")
                .isActive(true)
                .build());

        Room room = roomRepository.save(Room.builder()
                .name("Phòng chiếu 4")
                .roomType(RoomType.TWO_D)
                .theater(theater)
                .isActive(true)
                .build());

        showtimeToday = showtimeRepository.save(Showtime.builder()
                .movie(movie1)
                .room(room)
                .startTime(LocalDate.now().atTime(20, 15))
                .endTime(LocalDate.now().atTime(22, 3))
                .priceStandard(new BigDecimal("85000.00"))
                .priceVip(new BigDecimal("95000.00"))
                .priceCouple(new BigDecimal("160000.00"))
                .isActive(true)
                .build());

        showtimeTomorrow = showtimeRepository.save(Showtime.builder()
                .movie(movie2)
                .room(room)
                .startTime(LocalDate.now().plusDays(1).atTime(18, 0))
                .endTime(LocalDate.now().plusDays(1).atTime(21, 10))
                .priceStandard(new BigDecimal("90000.00"))
                .priceVip(new BigDecimal("100000.00"))
                .priceCouple(new BigDecimal("170000.00"))
                .isActive(true)
                .build());

        seatA1 = seatRepository.save(Seat.builder()
                .room(room)
                .rowName("A")
                .seatNumber(1)
                .seatType(SeatType.STANDARD)
                .build());

        seatA2 = seatRepository.save(Seat.builder()
                .room(room)
                .rowName("A")
                .seatNumber(2)
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
        resConfirmedA = reservationRepository.save(Reservation.builder()
                .bookingCode("BK-USERA-CONFIRMED-001")
                .user(userA)
                .showtime(showtimeToday)
                .totalPrice(new BigDecimal("235000.00"))
                .status(ReservationStatus.CONFIRMED)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build());

        reservedSeatRepository.save(ReservedSeat.builder()
                .reservation(resConfirmedA)
                .seat(seatA1)
                .price(new BigDecimal("85000.00"))
                .build());
        reservedSeatRepository.save(ReservedSeat.builder()
                .reservation(resConfirmedA)
                .seat(seatA2)
                .price(new BigDecimal("85000.00"))
                .build());

        orderItemRepository.save(OrderItem.builder()
                .reservation(resConfirmedA)
                .product(comboPopcorn)
                .unitPrice(new BigDecimal("65000.00"))
                .quantity(1)
                .subtotal(new BigDecimal("65000.00"))
                .build());

        paymentRepository.save(Payment.builder()
                .reservation(resConfirmedA)
                .transactionRef("TXN-USERA-001")
                .transactionNo("VNPAY-999001")
                .paymentMethod(PaymentMethod.VNPAY)
                .bankCode("NCB")
                .amount(new BigDecimal("235000.00"))
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .build());

        ticketService.generateTicketsForReservation(resConfirmedA);

        // 2. Pending Reservation for User A
        resPendingA = reservationRepository.save(Reservation.builder()
                .bookingCode("BK-USERA-PENDING-002")
                .user(userA)
                .showtime(showtimeToday)
                .totalPrice(new BigDecimal("85000.00"))
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build());

        reservedSeatRepository.save(ReservedSeat.builder()
                .reservation(resPendingA)
                .seat(seatA1)
                .price(new BigDecimal("85000.00"))
                .build());

        // 3. Cancelled Reservation for User A
        resCancelledA = reservationRepository.save(Reservation.builder()
                .bookingCode("BK-USERA-CANCELLED-003")
                .user(userA)
                .showtime(showtimeToday)
                .totalPrice(new BigDecimal("85000.00"))
                .status(ReservationStatus.CANCELLED)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build());

        // 4. Confirmed Reservation for User B (Tomorrow showtime)
        resConfirmedB = reservationRepository.save(Reservation.builder()
                .bookingCode("BK-USERB-CONFIRMED-004")
                .user(userB)
                .showtime(showtimeTomorrow)
                .totalPrice(new BigDecimal("90000.00"))
                .status(ReservationStatus.CONFIRMED)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build());

        reservedSeatRepository.save(ReservedSeat.builder()
                .reservation(resConfirmedB)
                .seat(seatA1)
                .price(new BigDecimal("90000.00"))
                .build());

        paymentRepository.save(Payment.builder()
                .reservation(resConfirmedB)
                .transactionRef("TXN-USERB-002")
                .transactionNo("VNPAY-999002")
                .paymentMethod(PaymentMethod.VNPAY)
                .bankCode("VISA")
                .amount(new BigDecimal("90000.00"))
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .build());

        ticketService.generateTicketsForReservation(resConfirmedB);
    }

    @Test
    @DisplayName("TC-01: Admin lấy danh sách booking thành công")
    void tc01_adminGetBookingListSuccessfully() {
        Page<AdminBookingListItemResponse> page = adminBookingService.getBookings(
                null, null, null, null, PageRequest.of(0, 20)
        );

        assertThat(page).isNotNull();
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("TC-02: Danh sách có pagination chính xác")
    void tc02_paginationWorksCorrectly() {
        Page<AdminBookingListItemResponse> page = adminBookingService.getBookings(
                null, null, null, null, PageRequest.of(0, 2)
        );

        assertThat(page.getContent().size()).isEqualTo(2);
        assertThat(page.getTotalPages()).isGreaterThanOrEqualTo(2);
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-03: Search theo bookingCode")
    void tc03_searchByBookingCode() {
        Page<AdminBookingListItemResponse> page = adminBookingService.getBookings(
                "BK-USERA-CONFIRMED-001", null, null, null, PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getBookingCode()).isEqualTo("BK-USERA-CONFIRMED-001");
        assertThat(page.getContent().get(0).getCustomerEmail()).isEqualTo("usera@example.com");
    }

    @Test
    @DisplayName("TC-04: Search theo customerName")
    void tc04_searchByCustomerName() {
        Page<AdminBookingListItemResponse> page = adminBookingService.getBookings(
                "Van A", null, null, null, PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allMatch(b -> b.getCustomerName().contains("Van A"));
    }

    @Test
    @DisplayName("TC-05: Search theo email")
    void tc05_searchByEmail() {
        Page<AdminBookingListItemResponse> page = adminBookingService.getBookings(
                "userb@example.com", null, null, null, PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCustomerEmail()).isEqualTo("userb@example.com");
    }

    @Test
    @DisplayName("TC-06: Search theo phone")
    void tc06_searchByPhone() {
        Page<AdminBookingListItemResponse> page = adminBookingService.getBookings(
                "0902222222", null, null, null, PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCustomerPhone()).isEqualTo("0902222222");
    }

    @Test
    @DisplayName("TC-07: Filter bookingStatus")
    void tc07_filterBookingStatus() {
        Page<AdminBookingListItemResponse> pageConfirmed = adminBookingService.getBookings(
                null, ReservationStatus.CONFIRMED, null, null, PageRequest.of(0, 10)
        );
        assertThat(pageConfirmed.getContent()).allMatch(b -> b.getBookingStatus() == ReservationStatus.CONFIRMED);

        Page<AdminBookingListItemResponse> pageCancelled = adminBookingService.getBookings(
                null, ReservationStatus.CANCELLED, null, null, PageRequest.of(0, 10)
        );
        assertThat(pageCancelled.getContent()).allMatch(b -> b.getBookingStatus() == ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("TC-08: Filter paymentStatus")
    void tc08_filterPaymentStatus() {
        Page<AdminBookingListItemResponse> pageCompleted = adminBookingService.getBookings(
                null, null, PaymentStatus.COMPLETED, null, PageRequest.of(0, 10)
        );

        assertThat(pageCompleted.getContent()).isNotEmpty();
        assertThat(pageCompleted.getContent()).allMatch(b -> b.getPaymentStatus() == PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("TC-09: Filter showtimeDate")
    void tc09_filterShowtimeDate() {
        Page<AdminBookingListItemResponse> pageToday = adminBookingService.getBookings(
                null, null, null, LocalDate.now(), PageRequest.of(0, 10)
        );

        assertThat(pageToday.getContent()).isNotEmpty();
        assertThat(pageToday.getContent()).allMatch(b -> b.getShowtimeStart().toLocalDate().equals(LocalDate.now()));

        Page<AdminBookingListItemResponse> pageTomorrow = adminBookingService.getBookings(
                null, null, null, LocalDate.now().plusDays(1), PageRequest.of(0, 10)
        );

        assertThat(pageTomorrow.getContent()).isNotEmpty();
        assertThat(pageTomorrow.getContent()).allMatch(b -> b.getShowtimeStart().toLocalDate().equals(LocalDate.now().plusDays(1)));
    }

    @Test
    @DisplayName("TC-10: Sort booking mới nhất trước")
    void tc10_sortByCreatedAtDesc() {
        Page<AdminBookingListItemResponse> page = adminBookingService.getBookings(
                null, null, null, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<AdminBookingListItemResponse> content = page.getContent();
        assertThat(content.size()).isGreaterThanOrEqualTo(2);
        for (int i = 0; i < content.size() - 1; i++) {
            LocalDateTime curr = content.get(i).getCreatedAt();
            LocalDateTime next = content.get(i + 1).getCreatedAt();
            if (curr != null && next != null) {
                assertThat(curr).isAfterOrEqualTo(next);
            }
        }
    }

    @Test
    @DisplayName("TC-11: Admin xem booking detail thành công")
    void tc11_adminGetBookingDetailSuccessfully() {
        AdminBookingDetailResponse detail = adminBookingService.getBookingDetail(resConfirmedA.getId());

        assertThat(detail).isNotNull();
        assertThat(detail.getReservationId()).isEqualTo(resConfirmedA.getId());
        assertThat(detail.getBookingCode()).isEqualTo("BK-USERA-CONFIRMED-001");
        assertThat(detail.getCustomerName()).isEqualTo("Nguyen Van A");
        assertThat(detail.getCustomerEmail()).isEqualTo("usera@example.com");
    }

    @Test
    @DisplayName("TC-12: Detail trả đầy đủ Customer + Movie + Showtime + Seats + F&B + Payment + Tickets")
    void tc12_detailContainsAllSections() {
        AdminBookingDetailResponse detail = adminBookingService.getBookingDetail(resConfirmedA.getId());

        // Customer
        assertThat(detail.getCustomerName()).isEqualTo("Nguyen Van A");
        assertThat(detail.getCustomerPhone()).isEqualTo("0901111111");

        // Movie
        assertThat(detail.getMovieTitle()).isEqualTo("ZOOTOPIA: PHI VU DONG TROI 2");
        assertThat(detail.getAgeRating()).isEqualTo(AgeRating.P);

        // Showtime
        assertThat(detail.getTheaterName()).isEqualTo("Trung Tam Chieu Phim Quoc Gia");
        assertThat(detail.getRoomName()).isEqualTo("Phòng chiếu 4");
        assertThat(detail.getRoomType()).isEqualTo("2D");

        // Seats
        assertThat(detail.getSeatNames()).containsExactlyInAnyOrder("A1", "A2");
        assertThat(detail.getTicketCount()).isEqualTo(2);

        // F&B
        assertThat(detail.getFnbItems()).hasSize(1);
        assertThat(detail.getFnbItems().get(0).getProductName()).isEqualTo("Combo 1 Bap + 1 Nuoc");
        assertThat(detail.getFnbSubtotal()).isEqualByComparingTo("65000.00");

        // Payment
        assertThat(detail.getPaymentMethod()).isEqualTo(PaymentMethod.VNPAY);
        assertThat(detail.getTransactionNo()).isEqualTo("VNPAY-999001");
        assertThat(detail.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

        // Tickets
        assertThat(detail.getTickets()).hasSize(2);
        assertThat(detail.getTotalAmount()).isEqualByComparingTo("235000.00");
    }

    @Test
    @DisplayName("TC-13: Detail có QR Code")
    void tc13_detailContainsQrCode() {
        AdminBookingDetailResponse detail = adminBookingService.getBookingDetail(resConfirmedA.getId());

        assertThat(detail.getTickets()).isNotEmpty();
        for (AdminBookingDetailResponse.TicketAdminDetail ticket : detail.getTickets()) {
            assertThat(ticket.getQrCodeUrl()).isNotBlank();
            assertThat(ticket.getQrCodeUrl()).startsWith("data:image/png;base64,");
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ISSUED);
        }
    }

    @Test
    @WithMockUser(username = "regular_user", roles = {"USER"})
    @DisplayName("TC-14: User không được truy cập Admin API => 403")
    void tc14_userCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/bookings"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/bookings/" + resConfirmedA.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-15: Anonymous không được truy cập Admin API => 403 Forbidden")
    void tc15_anonymousCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/bookings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-16: Booking không tồn tại => 404 ResourceNotFoundException")
    void tc16_bookingNotFoundReturns404() {
        assertThrows(ResourceNotFoundException.class, () -> {
            adminBookingService.getBookingDetail(9999999L);
        });
    }

    @Test
    @DisplayName("TC-17: Booking list không trả QR Code")
    void tc17_bookingListDoesNotContainQrCode() {
        Page<AdminBookingListItemResponse> page = adminBookingService.getBookings(
                null, null, null, null, PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).isNotEmpty();
        // Class AdminBookingListItemResponse does not even have qrCodeUrl or tickets list
        for (AdminBookingListItemResponse item : page.getContent()) {
            assertThat(item.getBookingCode()).isNotBlank();
            assertThat(item.getMovieTitle()).isNotBlank();
        }
    }

    @Test
    @DisplayName("TC-18: Admin xem được booking của User khác mà không bị chặn ownership")
    void tc18_adminCanAccessAnyUsersBooking() {
        // User B's booking
        AdminBookingDetailResponse detailB = adminBookingService.getBookingDetail(resConfirmedB.getId());

        assertThat(detailB).isNotNull();
        assertThat(detailB.getCustomerEmail()).isEqualTo("userb@example.com");
        assertThat(detailB.getBookingCode()).isEqualTo("BK-USERB-CONFIRMED-004");
    }

    @Test
    @DisplayName("TC-19: Booking không có F&B vẫn trả response hợp lệ")
    void tc19_bookingWithoutFnbReturnsValidResponse() {
        // User B has no F&B
        AdminBookingDetailResponse detailB = adminBookingService.getBookingDetail(resConfirmedB.getId());

        assertThat(detailB.getFnbItems()).isEmpty();
        assertThat(detailB.getFnbSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(detailB.getTotalAmount()).isEqualByComparingTo("90000.00");
    }

    @Test
    @DisplayName("TC-20: Booking chưa thanh toán/PENDING vẫn hiển thị đúng status")
    void tc20_pendingUnpaidBookingDisplayedCorrectly() {
        AdminBookingDetailResponse detailPending = adminBookingService.getBookingDetail(resPendingA.getId());

        assertThat(detailPending.getBookingStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(detailPending.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(detailPending.getTickets()).isEmpty();
    }
}
