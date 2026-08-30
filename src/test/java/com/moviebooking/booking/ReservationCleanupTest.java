package com.moviebooking.booking;

import com.moviebooking.dto.req.HoldSeatsRequest;
import com.moviebooking.dto.res.HoldSeatsResponse;
import com.moviebooking.exception.ReservationNotModifiableException;
import com.moviebooking.model.*;
import com.moviebooking.model.enums.*;
import com.moviebooking.repository.*;
import com.moviebooking.service.booking.AdminBookingService;
import com.moviebooking.service.booking.BookingService;
import com.moviebooking.service.payment.PaymentService;
import com.moviebooking.service.seat.ShowtimeSeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class ReservationCleanupTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ShowtimeSeatService showtimeSeatService;

    @Autowired
    private AdminBookingService adminBookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private ShowtimeSeatRepository showtimeSeatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User userA;
    private User userB;
    private Showtime showtime;
    private Seat seat1;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder()
                .email("usera_" + System.currentTimeMillis() + "@test.com")
                .password("Password123!")
                .firstName("User")
                .lastName("A")
                .phone("0900000001")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build());

        userB = userRepository.save(User.builder()
                .email("userb_" + System.currentTimeMillis() + "@test.com")
                .password("Password123!")
                .firstName("User")
                .lastName("B")
                .phone("0900000002")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build());

        Movie movie = movieRepository.save(Movie.builder()
                .title("CleanUp Test Movie")
                .description("Desc")
                .director("Dir")
                .actors("Actors")
                .duration(120)
                .releaseDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .posterPath("https://poster.jpg")
                .ageRating(AgeRating.P)
                .language("Vietnamese")
                .status(MovieStatus.NOW_SHOWING)
                .build());

        Theater theater = theaterRepository.save(Theater.builder()
                .name("CleanUp Theater")
                .address("123 St")
                .city("Hanoi")
                .district("Ba Dinh")
                .phone("0241234567")
                .isActive(true)
                .build());

        Room room = roomRepository.save(Room.builder()
                .name("Room CleanUp")
                .roomType(RoomType.TWO_D)
                .theater(theater)
                .isActive(true)
                .build());

        seat1 = seatRepository.save(Seat.builder()
                .room(room)
                .rowName("A")
                .seatNumber(1)
                .seatType(SeatType.STANDARD)
                .build());

        showtime = showtimeRepository.save(Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(LocalDateTime.now().plusDays(1).with(LocalTime.of(18, 0)))
                .endTime(LocalDateTime.now().plusDays(1).with(LocalTime.of(20, 0)))
                .priceStandard(BigDecimal.valueOf(90000))
                .priceVip(BigDecimal.valueOf(110000))
                .priceCouple(BigDecimal.valueOf(180000))
                .isActive(true)
                .isOnlineSelling(true)
                .build());

        showtimeSeatService.initializeSeatsForShowtime(showtime);
    }

    @Test
    @DisplayName("TC-01: Đơn PENDING quá hạn tự động chuyển EXPIRED, payment FAILED, ghế AVAILABLE")
    void tc01_expiredPendingReservationIsCleanedUp() {
        HoldSeatsResponse hold = showtimeSeatService.holdSeats(HoldSeatsRequest.builder()
                .showtimeId(showtime.getId())
                .seatIds(List.of(seat1.getId()))
                .build(), userA);

        Reservation res = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-TC01-" + System.currentTimeMillis())
                .user(userA)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(90000))
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // Already expired
                .build());

        Payment payment = paymentRepository.save(Payment.builder()
                .reservation(res)
                .paymentMethod(PaymentMethod.VNPAY)
                .transactionRef("TXN-TC01-" + System.currentTimeMillis())
                .amount(BigDecimal.valueOf(90000))
                .status(PaymentStatus.PENDING)
                .build());

        ShowtimeSeat ss = showtimeSeatRepository.findByShowtimeIdAndSeatId(showtime.getId(), seat1.getId()).orElseThrow();
        ss.setStatus(ShowtimeSeatStatus.HELD);
        ss.setHoldToken(hold.getHoldToken());
        ss.setHeldByUser(userA);
        ss.setReservation(res);
        showtimeSeatRepository.save(ss);

        int count = bookingService.cleanupExpiredReservations();
        assertThat(count).isGreaterThanOrEqualTo(1);

        Reservation updatedRes = reservationRepository.findById(res.getId()).orElseThrow();
        assertThat(updatedRes.getStatus()).isEqualTo(ReservationStatus.EXPIRED);

        Payment updatedPay = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updatedPay.getStatus()).isEqualTo(PaymentStatus.FAILED);

        ShowtimeSeat updatedSs = showtimeSeatRepository.findById(ss.getId()).orElseThrow();
        assertThat(updatedSs.getStatus()).isEqualTo(ShowtimeSeatStatus.AVAILABLE);
        assertThat(updatedSs.getHoldToken()).isNull();
        assertThat(updatedSs.getReservation()).isNull();
    }

    @Test
    @DisplayName("TC-02: Đơn PENDING còn thời hạn KHÔNG bị dọn dẹp")
    void tc02_unexpiredPendingReservationIsNotCleanedUp() {
        Reservation res = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-TC02-" + System.currentTimeMillis())
                .user(userA)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(90000))
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5)) // Active
                .build());

        bookingService.cleanupExpiredReservations();

        Reservation updatedRes = reservationRepository.findById(res.getId()).orElseThrow();
        assertThat(updatedRes.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("TC-03: Đơn CONFIRMED dù quá hạn mốc expiresAt vẫn KHÔNG bị dọn dẹp")
    void tc03_confirmedReservationIsNotCleanedUp() {
        Reservation res = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-TC03-" + System.currentTimeMillis())
                .user(userA)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(90000))
                .status(ReservationStatus.CONFIRMED)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build());

        bookingService.cleanupExpiredReservations();

        Reservation updatedRes = reservationRepository.findById(res.getId()).orElseThrow();
        assertThat(updatedRes.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("TC-04: Đơn CANCELLED không bị dọn dẹp lại")
    void tc04_cancelledReservationIsNotCleanedUp() {
        Reservation res = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-TC04-" + System.currentTimeMillis())
                .user(userA)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(90000))
                .status(ReservationStatus.CANCELLED)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build());

        bookingService.cleanupExpiredReservations();

        Reservation updatedRes = reservationRepository.findById(res.getId()).orElseThrow();
        assertThat(updatedRes.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("TC-05: Thử tạo payment url cho đơn EXPIRED => bị từ chối")
    void tc05_createPaymentForExpiredReservationRejected() {
        Reservation res = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-TC05-" + System.currentTimeMillis())
                .user(userA)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(90000))
                .status(ReservationStatus.EXPIRED)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build());

        assertThrows(ReservationNotModifiableException.class, () -> {
            paymentService.createPaymentUrl(res.getId(), userA, "127.0.0.1");
        });
    }

    @Test
    @DisplayName("TC-06: Thử tạo payment url cho đơn CANCELLED => bị từ chối")
    void tc06_createPaymentForCancelledReservationRejected() {
        Reservation res = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-TC06-" + System.currentTimeMillis())
                .user(userA)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(90000))
                .status(ReservationStatus.CANCELLED)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build());

        assertThrows(ReservationNotModifiableException.class, () -> {
            paymentService.createPaymentUrl(res.getId(), userA, "127.0.0.1");
        });
    }

    @Test
    @DisplayName("TC-07: Callback VNPAY trả SUCCESS cho đơn đã EXPIRED => bị từ chối confirm")
    void tc07_vnpaySuccessForExpiredReservationRejected() {
        Reservation res = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-TC07-" + System.currentTimeMillis())
                .user(userA)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(90000))
                .status(ReservationStatus.EXPIRED)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build());

        Payment payment = paymentRepository.save(Payment.builder()
                .reservation(res)
                .paymentMethod(PaymentMethod.VNPAY)
                .transactionRef("TXN-TC07-" + System.currentTimeMillis())
                .amount(BigDecimal.valueOf(90000))
                .status(PaymentStatus.FAILED)
                .build());

        assertThrows(ReservationNotModifiableException.class, () -> {
            paymentService.confirmPrimaryBookingTransaction(payment.getId(), payment.getTransactionRef(), "9999", "NCB");
        });
    }

    @Test
    @DisplayName("TC-08: Sau khi Reservation A bị EXPIRED, User B có thể giữ lại cùng ghế")
    void tc08_userBCanHoldSeatAfterUserAExpired() {
        HoldSeatsResponse holdA = showtimeSeatService.holdSeats(HoldSeatsRequest.builder()
                .showtimeId(showtime.getId())
                .seatIds(List.of(seat1.getId()))
                .build(), userA);

        Reservation resA = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-TC08-" + System.currentTimeMillis())
                .user(userA)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(90000))
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build());

        ShowtimeSeat ssA = showtimeSeatRepository.findByShowtimeIdAndSeatId(showtime.getId(), seat1.getId()).orElseThrow();
        ssA.setReservation(resA);
        showtimeSeatRepository.save(ssA);

        bookingService.cleanupExpiredReservations();

        // User B holds seat
        HoldSeatsResponse holdB = showtimeSeatService.holdSeats(HoldSeatsRequest.builder()
                .showtimeId(showtime.getId())
                .seatIds(List.of(seat1.getId()))
                .build(), userB);

        assertThat(holdB.getHoldToken()).isNotEqualTo(holdA.getHoldToken());

        ShowtimeSeat ss = showtimeSeatRepository.findByShowtimeIdAndSeatId(showtime.getId(), seat1.getId()).orElseThrow();
        assertThat(ss.getHeldByUser().getId()).isEqualTo(userB.getId());
        assertThat(ss.getHoldToken()).isEqualTo(holdB.getHoldToken());
    }

    @Test
    @DisplayName("TC-09: Ghế đã bị User B giữ không bị tác động sai bởi cleanup của đơn cũ")
    void tc09_userBHeldSeatNotMistakenlyReleasedByOldCleanup() {
        Reservation resA = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-TC09-" + System.currentTimeMillis())
                .user(userA)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(90000))
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build());

        // Seat is held by User B with holdToken B
        HoldSeatsResponse holdB = showtimeSeatService.holdSeats(HoldSeatsRequest.builder()
                .showtimeId(showtime.getId())
                .seatIds(List.of(seat1.getId()))
                .build(), userB);

        bookingService.cleanupExpiredReservations();

        ShowtimeSeat ss = showtimeSeatRepository.findByShowtimeIdAndSeatId(showtime.getId(), seat1.getId()).orElseThrow();
        assertThat(ss.getStatus()).isEqualTo(ShowtimeSeatStatus.HELD);
        assertThat(ss.getHeldByUser().getId()).isEqualTo(userB.getId());
        assertThat(ss.getHoldToken()).isEqualTo(holdB.getHoldToken());
    }

    @Test
    @DisplayName("TC-10: Idempotency - Chạy Scheduler liên tiếp không gây side effect")
    void tc10_idempotentSchedulerCleanup() {
        Reservation res = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-TC10-" + System.currentTimeMillis())
                .user(userA)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(90000))
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build());

        int count1 = bookingService.cleanupExpiredReservations();
        assertThat(count1).isGreaterThanOrEqualTo(1);

        int count2 = bookingService.cleanupExpiredReservations();
        assertThat(count2).isEqualTo(0);
    }

    @Test
    @DisplayName("TC-11: Admin thấy trạng thái EXPIRED & FAILED thay vì PENDING sau cleanup")
    void tc11_adminSeeExpiredAndFailedAfterCleanup() {
        Reservation res = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-TC11-" + System.currentTimeMillis())
                .user(userA)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(90000))
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build());

        paymentRepository.save(Payment.builder()
                .reservation(res)
                .paymentMethod(PaymentMethod.VNPAY)
                .transactionRef("TXN-TC11-" + System.currentTimeMillis())
                .amount(BigDecimal.valueOf(90000))
                .status(PaymentStatus.PENDING)
                .build());

        bookingService.cleanupExpiredReservations();

        var adminPage = adminBookingService.getBookings("REV-TC11", null, null, null, PageRequest.of(0, 10, Sort.by("createdAt").descending()));
        assertThat(adminPage.getContent()).hasSize(1);
        assertThat(adminPage.getContent().get(0).getBookingStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(adminPage.getContent().get(0).getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("TC-12: bookingCode của Reservation EXPIRED được giữ nguyên cho Audit")
    void tc12_bookingCodePreservedForExpiredReservation() {
        String originalBookingCode = "REV-TC12-" + System.currentTimeMillis();
        Reservation res = reservationRepository.save(Reservation.builder()
                .bookingCode(originalBookingCode)
                .user(userA)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(90000))
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build());

        bookingService.cleanupExpiredReservations();

        Reservation updatedRes = reservationRepository.findById(res.getId()).orElseThrow();
        assertThat(updatedRes.getBookingCode()).isEqualTo(originalBookingCode);
        assertThat(updatedRes.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }
}
