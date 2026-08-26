package com.moviebooking.booking;

import com.moviebooking.model.*;
import com.moviebooking.model.enums.*;
import com.moviebooking.repository.*;
import com.moviebooking.service.booking.TicketService;
import com.moviebooking.service.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class Phase5TicketTest {

    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private ShowtimeSeatRepository showtimeSeatRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private PaymentService paymentService;
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

    private Reservation testReservation;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("test5@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User 5")
                .phone("0987654321")
                .dateOfBirth(java.time.LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build();
        userRepository.save(user);

        Movie movie = Movie.builder()
                .title("Ticket Test Movie")
                .description("Test")
                .director("Test Director")
                .actors("Test Actors")
                .duration(120)
                .releaseDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now().plusMonths(1))
                .posterPath("/poster.jpg")
                .ageRating(com.moviebooking.model.enums.AgeRating.P)
                .language("Tiếng Việt")
                .status(MovieStatus.NOW_SHOWING)
                .build();
        movieRepository.save(movie);

        Theater theater = Theater.builder()
                .name("Ticket Test Theater")
                .address("123 Test St")
                .city("Hanoi")
                .district("Cau Giay")
                .phone("0123456789")
                .isActive(true)
                .build();
        theaterRepository.save(theater);

        Room room = Room.builder()
                .name("Ticket Room")
                .theater(theater)
                .roomType(com.moviebooking.model.enums.RoomType.TWO_D)
                .isActive(true)
                .build();
        roomRepository.save(room);

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .priceStandard(BigDecimal.valueOf(50000))
                .priceVip(BigDecimal.valueOf(80000))
                .priceCouple(BigDecimal.valueOf(100000))
                .isActive(true)
                .build();
        showtimeRepository.save(showtime);

        Seat seat1 = Seat.builder().room(room).rowName("A").seatNumber(1).seatType(SeatType.STANDARD).isActive(true).build();
        Seat seat2 = Seat.builder().room(room).rowName("A").seatNumber(2).seatType(SeatType.STANDARD).isActive(true).build();
        seatRepository.saveAll(List.of(seat1, seat2));

        Reservation reservation = Reservation.builder()
                .user(user)
                .showtime(showtime)
                .totalPrice(BigDecimal.valueOf(100000))
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .bookingCode("BK-123456789")
                .build();
        testReservation = reservationRepository.save(reservation);

        ShowtimeSeat ss1 = ShowtimeSeat.builder()
                .showtime(showtime)
                .seat(seat1)
                .price(BigDecimal.valueOf(50000))
                .status(ShowtimeSeatStatus.HELD)
                .heldByUser(user)
                .lockedUntil(LocalDateTime.now().plusMinutes(10))
                .reservation(reservation)
                .build();
        ShowtimeSeat ss2 = ShowtimeSeat.builder()
                .showtime(showtime)
                .seat(seat2)
                .price(BigDecimal.valueOf(50000))
                .status(ShowtimeSeatStatus.HELD)
                .heldByUser(user)
                .lockedUntil(LocalDateTime.now().plusMinutes(10))
                .reservation(reservation)
                .build();
        showtimeSeatRepository.saveAll(List.of(ss1, ss2));

        ReservedSeat rs1 = ReservedSeat.builder()
                .reservation(reservation)
                .seat(seat1)
                .price(BigDecimal.valueOf(50000))
                .build();
        ReservedSeat rs2 = ReservedSeat.builder()
                .reservation(reservation)
                .seat(seat2)
                .price(BigDecimal.valueOf(50000))
                .build();
        reservedSeatRepository.saveAll(List.of(rs1, rs2));

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(reservation.getTotalPrice());
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionRef("TXN-TEST-12345");
        testPayment = paymentRepository.save(payment);
    }

    @Test
    void testTicketGenerationOnPaymentConfirmation() {
        // Act
        paymentService.confirmPrimaryBookingTransaction(testPayment.getId(), testPayment.getTransactionRef(), "VNPAY-123", "NCB");

        // Assert
        Payment updatedPayment = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        Reservation updatedReservation = reservationRepository.findById(testReservation.getId()).orElseThrow();
        assertThat(updatedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        List<Ticket> tickets = ticketRepository.findByReservationId(testReservation.getId());
        assertThat(tickets).hasSize(2);

        Ticket ticket = tickets.get(0);
        assertThat(ticket.getTicketCode()).startsWith("TKT-");
        assertThat(ticket.getQrCodeUrl()).startsWith("data:image/png;base64,");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ISSUED);
        assertThat(ticket.getCheckedInAt()).isNull();
    }
}
