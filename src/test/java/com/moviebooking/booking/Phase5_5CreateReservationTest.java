package com.moviebooking.booking;

import com.moviebooking.dto.req.CreateReservationRequest;
import com.moviebooking.dto.res.ReservationReviewResponse;
import com.moviebooking.exception.*;
import com.moviebooking.model.*;
import com.moviebooking.model.enums.*;
import com.moviebooking.repository.*;
import com.moviebooking.service.booking.BookingService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class Phase5_5CreateReservationTest {

    @Autowired
    private BookingService bookingService;

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
    private ReservedSeatRepository reservedSeatRepository;

    private User user1;
    private User user2;
    private Showtime showtime;
    private Seat seatA1;
    private Seat seatA2;
    private ShowtimeSeat showtimeSeatA1;
    private ShowtimeSeat showtimeSeatA2;
    private String holdToken;
    private LocalDateTime expiresAt;

    @BeforeEach
    void setUp() {
        user1 = userRepository.findByEmail("user1_p55@test.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("user1_p55@test.com")
                        .password("password")
                        .firstName("User")
                        .lastName("One")
                        .phone("0900000001")
                        .dateOfBirth(LocalDate.of(2000, 1, 1))
                        .gender(Gender.MALE)
                        .role(Role.USER)
                        .build())
        );

        user2 = userRepository.findByEmail("user2_p55@test.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("user2_p55@test.com")
                        .password("password")
                        .firstName("User")
                        .lastName("Two")
                        .phone("0900000002")
                        .dateOfBirth(LocalDate.of(2000, 1, 1))
                        .gender(Gender.FEMALE)
                        .role(Role.USER)
                        .build())
        );

        Movie movie = movieRepository.save(Movie.builder()
                .title("Phase 5.5 Test Movie")
                .description("Description")
                .director("Director")
                .actors("Actors")
                .duration(120)
                .releaseDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusMonths(1))
                .posterPath("/poster.jpg")
                .ageRating(AgeRating.P)
                .language("Tiếng Việt")
                .status(MovieStatus.NOW_SHOWING)
                .build());

        Theater theater = theaterRepository.save(Theater.builder()
                .name("Phase 5.5 Theater")
                .address("123 Street")
                .city("Hanoi")
                .district("Cau Giay")
                .phone("0123456789")
                .isActive(true)
                .build());

        Room room = roomRepository.save(Room.builder()
                .name("Room 5.5")
                .theater(theater)
                .roomType(RoomType.TWO_D)
                .isActive(true)
                .build());

        seatA1 = seatRepository.save(Seat.builder()
                .room(room)
                .rowName("A")
                .seatNumber(1)
                .seatType(SeatType.STANDARD)
                .isActive(true)
                .build());

        seatA2 = seatRepository.save(Seat.builder()
                .room(room)
                .rowName("A")
                .seatNumber(2)
                .seatType(SeatType.VIP)
                .isActive(true)
                .build());

        showtime = showtimeRepository.save(Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusHours(4))
                .priceStandard(new BigDecimal("90000.00"))
                .priceVip(new BigDecimal("110000.00"))
                .priceCouple(new BigDecimal("150000.00"))
                .isActive(true)
                .build());

        holdToken = UUID.randomUUID().toString();
        expiresAt = LocalDateTime.now().plusMinutes(8);

        showtimeSeatA1 = showtimeSeatRepository.save(ShowtimeSeat.builder()
                .showtime(showtime)
                .seat(seatA1)
                .status(ShowtimeSeatStatus.HELD)
                .holdToken(holdToken)
                .heldByUser(user1)
                .lockedUntil(expiresAt)
                .price(new BigDecimal("90000.00"))
                .build());

        showtimeSeatA2 = showtimeSeatRepository.save(ShowtimeSeat.builder()
                .showtime(showtime)
                .seat(seatA2)
                .status(ShowtimeSeatStatus.HELD)
                .holdToken(holdToken)
                .heldByUser(user1)
                .lockedUntil(expiresAt)
                .price(new BigDecimal("110000.00"))
                .build());
    }

    @Test
    @DisplayName("1. Create Reservation successfully with status PENDING")
    void testCreateReservationSuccess() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .showtimeId(showtime.getId())
                .seatIds(List.of(seatA1.getId(), seatA2.getId()))
                .holdToken(holdToken)
                .build();

        ReservationReviewResponse response = bookingService.createReservation(request, user1);

        assertNotNull(response);
        assertNotNull(response.getReservationId());
        assertEquals(ReservationStatus.PENDING, response.getStatus());
        assertEquals("Phase 5.5 Test Movie", response.getMovieTitle());
        assertEquals(new BigDecimal("200000.00"), response.getTotalAmount());
        assertEquals(new BigDecimal("200000.00"), response.getTicketSubtotal());
        assertEquals(2, response.getTicketSeats().size());
        assertEquals(expiresAt, response.getExpiresAt());

        // Verify seats in DB remain HELD and are linked to reservation
        ShowtimeSeat ss1 = showtimeSeatRepository.findById(showtimeSeatA1.getId()).orElseThrow();
        ShowtimeSeat ss2 = showtimeSeatRepository.findById(showtimeSeatA2.getId()).orElseThrow();
        assertEquals(ShowtimeSeatStatus.HELD, ss1.getStatus());
        assertEquals(ShowtimeSeatStatus.HELD, ss2.getStatus());
        assertEquals(response.getReservationId(), ss1.getReservation().getId());
        assertEquals(response.getReservationId(), ss2.getReservation().getId());
    }

    @Test
    @DisplayName("2. Invalid holdToken rejected")
    void testInvalidHoldTokenRejected() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .showtimeId(showtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .holdToken("INVALID_TOKEN")
                .build();

        assertThrows(InvalidSeatHoldException.class, () ->
                bookingService.createReservation(request, user1)
        );
    }

    @Test
    @DisplayName("3. Expired holdToken rejected")
    void testExpiredHoldTokenRejected() {
        showtimeSeatA1.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        showtimeSeatRepository.save(showtimeSeatA1);

        CreateReservationRequest request = CreateReservationRequest.builder()
                .showtimeId(showtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .holdToken(holdToken)
                .build();

        assertThrows(InvalidSeatHoldException.class, () ->
                bookingService.createReservation(request, user1)
        );
    }

    @Test
    @DisplayName("4. Hold belonging to another user rejected")
    void testHoldBelongingToAnotherUserRejected() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .showtimeId(showtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .holdToken(holdToken)
                .build();

        assertThrows(SeatHoldOwnershipException.class, () ->
                bookingService.createReservation(request, user2)
        );
    }

    @Test
    @DisplayName("5. Seat not belonging to showtime rejected")
    void testSeatNotBelongingToShowtimeRejected() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .showtimeId(showtime.getId())
                .seatIds(List.of(99999L))
                .holdToken(holdToken)
                .build();

        assertThrows(InvalidSeatHoldException.class, () ->
                bookingService.createReservation(request, user1)
        );
    }

    @Test
    @DisplayName("6. Seat not HELD rejected")
    void testSeatNotHeldRejected() {
        showtimeSeatA1.setStatus(ShowtimeSeatStatus.AVAILABLE);
        showtimeSeatRepository.save(showtimeSeatA1);

        CreateReservationRequest request = CreateReservationRequest.builder()
                .showtimeId(showtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .holdToken(holdToken)
                .build();

        assertThrows(InvalidSeatHoldException.class, () ->
                bookingService.createReservation(request, user1)
        );
    }

    @Test
    @DisplayName("7. SOLD seat rejected")
    void testSoldSeatRejected() {
        showtimeSeatA1.setStatus(ShowtimeSeatStatus.SOLD);
        showtimeSeatRepository.save(showtimeSeatA1);

        CreateReservationRequest request = CreateReservationRequest.builder()
                .showtimeId(showtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .holdToken(holdToken)
                .build();

        assertThrows(SeatAlreadyReservedException.class, () ->
                bookingService.createReservation(request, user1)
        );
    }

    @Test
    @DisplayName("8. Duplicate request returns existing reservation (Idempotency)")
    void testDuplicateRequestReturnsExistingReservation() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .showtimeId(showtime.getId())
                .seatIds(List.of(seatA1.getId(), seatA2.getId()))
                .holdToken(holdToken)
                .build();

        ReservationReviewResponse res1 = bookingService.createReservation(request, user1);
        ReservationReviewResponse res2 = bookingService.createReservation(request, user1);

        assertNotNull(res1);
        assertNotNull(res2);
        assertEquals(res1.getReservationId(), res2.getReservationId());
        assertEquals(res1.getBookingCode(), res2.getBookingCode());
    }

    @Test
    @DisplayName("9. Non-existent showtime rejected")
    void testNonExistentShowtimeRejected() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .showtimeId(999999L)
                .seatIds(List.of(seatA1.getId()))
                .holdToken(holdToken)
                .build();

        assertThrows(ResourceNotFoundException.class, () ->
                bookingService.createReservation(request, user1)
        );
    }
}
