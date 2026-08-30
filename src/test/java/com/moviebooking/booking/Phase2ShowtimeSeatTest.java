package com.moviebooking.booking;

import com.moviebooking.dto.req.HoldSeatsRequest;
import com.moviebooking.dto.req.ReleaseSeatsRequest;
import com.moviebooking.dto.res.HoldSeatsResponse;
import com.moviebooking.dto.res.PublicShowtimeSeatResponse;
import com.moviebooking.exception.*;
import com.moviebooking.model.*;
import com.moviebooking.model.enums.*;
import com.moviebooking.repository.*;
import com.moviebooking.service.seat.ShowtimeSeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class Phase2ShowtimeSeatTest {



    @Autowired
    private ShowtimeSeatService showtimeSeatService;

    @Autowired
    private ShowtimeSeatRepository showtimeSeatRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservedSeatRepository reservedSeatRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User userA;
    private User userB;
    private Room room50;
    private Movie testMovie;
    private Showtime futureShowtime;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        paymentRepository.deleteAll();
        reservedSeatRepository.deleteAll();
        reservationRepository.deleteAll();
        showtimeSeatRepository.deleteAll();
        showtimeRepository.deleteAll();


        userA = userRepository.findByEmail("usera_phase2@example.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("usera_phase2@example.com")
                        .password("password")
                        .firstName("User")
                        .lastName("A")
                        .phone("0900000001")
                        .dateOfBirth(java.time.LocalDate.of(2000, 1, 1))
                        .gender(Gender.MALE)
                        .role(Role.USER)
                        .build())
        );

        userB = userRepository.findByEmail("userb_phase2@example.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("userb_phase2@example.com")
                        .password("password")
                        .firstName("User")
                        .lastName("B")
                        .phone("0900000002")
                        .dateOfBirth(java.time.LocalDate.of(2000, 1, 1))
                        .gender(Gender.FEMALE)
                        .role(Role.USER)
                        .build())
        );

        Theater theater = theaterRepository.findAll().stream().findFirst().orElseGet(() ->
                theaterRepository.save(Theater.builder()
                        .name("CineMind Central")
                        .address("87 Lăng Bác")
                        .city("Hà Nội")
                        .district("Ba Đình")
                        .email("contact@cinemind.vn")
                        .phone("0241234567")
                        .isActive(true)
                        .build())
        );

        room50 = roomRepository.findAll().stream().filter(r -> "Phòng chiếu Test Phase2".equals(r.getName())).findFirst().orElseGet(() -> {
            Room r = roomRepository.save(Room.builder()
                    .name("Phòng chiếu Test Phase2")
                    .roomType(RoomType.TWO_D)
                    .theater(theater)
                    .isActive(true)
                    .build());



            // Create 50 seats (5 rows x 10 seats)
            List<Seat> seats = new ArrayList<>();
            String[] rows = {"A", "B", "C", "D", "E"};
            for (String row : rows) {
                for (int i = 1; i <= 10; i++) {
                    SeatType type = "E".equals(row) ? SeatType.COUPLE : ("D".equals(row) ? SeatType.VIP : SeatType.STANDARD);
                    seats.add(Seat.builder()
                            .room(r)
                            .rowName(row)
                            .seatNumber(i)
                            .seatType(type)
                            .build());
                }
            }
            seatRepository.saveAll(seats);
            return r;
        });

        testMovie = movieRepository.findAll().stream().findFirst().orElseGet(() ->
                movieRepository.save(Movie.builder()
                        .title("Test Movie Phase 2")
                        .description("Mô tả phim test Phase 2")
                        .duration(120)
                        .director("Director Test")
                        .actors("Actor 1, Actor 2")
                        .language("Tiếng Việt")
                        .posterPath("/posters/test.jpg")
                        .bannerPath("/banners/test.jpg")
                        .ageRating(AgeRating.P)
                        .status(MovieStatus.NOW_SHOWING)
                        .releaseDate(java.time.LocalDate.now().minusDays(5))
                        .endDate(java.time.LocalDate.now().plusDays(30))
                        .build())
        );





        futureShowtime = showtimeRepository.save(Showtime.builder()
                .movie(testMovie)
                .room(room50)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .priceStandard(new BigDecimal("90000.00"))
                .priceVip(new BigDecimal("95000.00"))
                .priceCouple(new BigDecimal("100000.00"))
                .isActive(true)
                .isOnlineSelling(true)
                .build());
    }

    @Test
    @DisplayName("TEST 1: Initialize ShowtimeSeat for 50 seats")
    void testInitialization50Seats() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeIdOrderBySeatRowNameAscSeatSeatNumberAsc(futureShowtime.getId());
        assertEquals(50, seats.size());
    }

    @Test
    @DisplayName("TEST 2: Idempotent initialization")
    void testIdempotentInitialization() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);

        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeIdOrderBySeatRowNameAscSeatSeatNumberAsc(futureShowtime.getId());
        assertEquals(50, seats.size());
    }

    @Test
    @Transactional
    @DisplayName("TEST 3: Price snapshot immutability")
    void testPriceSnapshotImmutability() {

        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        List<ShowtimeSeat> seatsBefore = showtimeSeatRepository.findByShowtimeIdOrderBySeatRowNameAscSeatSeatNumberAsc(futureShowtime.getId());
        BigDecimal originalVipPrice = seatsBefore.stream()
                .filter(s -> s.getSeat().getSeatType() == SeatType.VIP)
                .findFirst().orElseThrow().getPrice();

        assertEquals(new BigDecimal("95000.00"), originalVipPrice);

        // Update Showtime prices
        futureShowtime.setPriceVip(new BigDecimal("120000.00"));
        showtimeRepository.save(futureShowtime);

        // Re-run initialization (idempotent)
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);

        List<ShowtimeSeat> seatsAfter = showtimeSeatRepository.findByShowtimeIdOrderBySeatRowNameAscSeatSeatNumberAsc(futureShowtime.getId());
        BigDecimal vipPriceAfter = seatsAfter.stream()
                .filter(s -> s.getSeat().getSeatType() == SeatType.VIP)
                .findFirst().orElseThrow().getPrice();

        // Price snapshot MUST remain 95000.00
        assertEquals(new BigDecimal("95000.00"), vipPriceAfter);
    }

    @Test
    @DisplayName("TEST 4: Successful hold single seat")
    void testSuccessfulHoldSingleSeat() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        Seat seatA1 = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()).get(0);

        HoldSeatsRequest req = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build();

        HoldSeatsResponse response = showtimeSeatService.holdSeats(req, userA);

        assertNotNull(response.getHoldToken());
        assertEquals(1, response.getHeldSeats().size());
        assertTrue(response.getExpiresAt().isAfter(LocalDateTime.now()));

        ShowtimeSeat dbSeat = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA1.getId()).orElseThrow();
        assertEquals(ShowtimeSeatStatus.HELD, dbSeat.getStatus());
        assertEquals(userA.getId(), dbSeat.getHeldByUser().getId());
        assertEquals(response.getHoldToken(), dbSeat.getHoldToken());
    }

    @Test
    @DisplayName("TEST 5: Hold multiple seats with same holdToken")
    void testHoldMultipleSeats() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        List<Seat> physicalSeats = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId());
        List<Long> seatIds = List.of(physicalSeats.get(0).getId(), physicalSeats.get(1).getId(), physicalSeats.get(2).getId());

        HoldSeatsRequest req = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(seatIds)
                .build();

        HoldSeatsResponse response = showtimeSeatService.holdSeats(req, userA);

        assertEquals(3, response.getHeldSeats().size());

        List<ShowtimeSeat> dbSeats = showtimeSeatRepository.findByShowtimeIdAndSeatIdIn(futureShowtime.getId(), seatIds);
        assertEquals(3, dbSeats.size());
        for (ShowtimeSeat ss : dbSeats) {
            assertEquals(ShowtimeSeatStatus.HELD, ss.getStatus());
            assertEquals(response.getHoldToken(), ss.getHoldToken());
            assertEquals(userA.getId(), ss.getHeldByUser().getId());
        }
    }

    @Test
    @DisplayName("TEST 6: Cannot hold SOLD seat")
    void testCannotHoldSoldSeat() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        Seat seatA1 = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()).get(0);

        ShowtimeSeat ss = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA1.getId()).orElseThrow();
        ss.setStatus(ShowtimeSeatStatus.SOLD);
        showtimeSeatRepository.save(ss);

        HoldSeatsRequest req = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build();

        assertThrows(SeatAlreadyReservedException.class, () -> showtimeSeatService.holdSeats(req, userA));
    }

    @Test
    @DisplayName("TEST 7: Cannot hold active HELD seat by another user")
    void testCannotHoldActiveHeldSeat() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        Seat seatA1 = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()).get(0);

        HoldSeatsRequest reqA = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build();

        showtimeSeatService.holdSeats(reqA, userA);

        HoldSeatsRequest reqB = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build();

        assertThrows(SeatAlreadyReservedException.class, () -> showtimeSeatService.holdSeats(reqB, userB));
    }

    @Test
    @DisplayName("TEST 8: Expired hold becomes AVAILABLE in Public Seat Map")
    void testExpiredHoldBecomesAvailableInSeatMap() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        Seat seatA1 = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()).get(0);

        ShowtimeSeat ss = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA1.getId()).orElseThrow();
        ss.setStatus(ShowtimeSeatStatus.HELD);
        ss.setHoldToken(UUID.randomUUID().toString());
        ss.setHeldByUser(userA);
        ss.setLockedUntil(LocalDateTime.now().minusMinutes(1)); // Expired
        showtimeSeatRepository.save(ss);

        List<PublicShowtimeSeatResponse> map = showtimeSeatService.getPublicSeatMap(futureShowtime.getId());
        PublicShowtimeSeatResponse seatA1Res = map.stream().filter(s -> s.getSeatId().equals(seatA1.getId())).findFirst().orElseThrow();

        assertEquals(ShowtimeSeatStatus.AVAILABLE, seatA1Res.getStatus());
    }

    @Test
    @DisplayName("TEST 9: Reclaim expired hold successfully")
    void testReclaimExpiredHold() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        Seat seatA1 = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()).get(0);

        ShowtimeSeat ss = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA1.getId()).orElseThrow();
        ss.setStatus(ShowtimeSeatStatus.HELD);
        ss.setHoldToken("OLD-TOKEN");
        ss.setHeldByUser(userA);
        ss.setLockedUntil(LocalDateTime.now().minusMinutes(1)); // Expired
        showtimeSeatRepository.save(ss);

        HoldSeatsRequest reqB = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build();

        HoldSeatsResponse responseB = showtimeSeatService.holdSeats(reqB, userB);
        assertNotNull(responseB.getHoldToken());
        assertNotEquals("OLD-TOKEN", responseB.getHoldToken());

        ShowtimeSeat dbSeat = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA1.getId()).orElseThrow();
        assertEquals(userB.getId(), dbSeat.getHeldByUser().getId());
    }

    @Test
    @DisplayName("TEST 10: Release seats by owner")
    void testReleaseSeatsByOwner() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        Seat seatA1 = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()).get(0);

        HoldSeatsRequest holdReq = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build();

        HoldSeatsResponse holdRes = showtimeSeatService.holdSeats(holdReq, userA);

        ReleaseSeatsRequest relReq = ReleaseSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .holdToken(holdRes.getHoldToken())
                .build();

        showtimeSeatService.releaseSeats(relReq, userA);

        ShowtimeSeat dbSeat = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA1.getId()).orElseThrow();
        assertEquals(ShowtimeSeatStatus.AVAILABLE, dbSeat.getStatus());
        assertNull(dbSeat.getHoldToken());
        assertNull(dbSeat.getHeldByUser());
        assertNull(dbSeat.getLockedUntil());
    }

    @Test
    @DisplayName("TEST 11: Release seats by wrong user")
    void testReleaseSeatsByWrongUser() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        Seat seatA1 = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()).get(0);

        HoldSeatsRequest holdReq = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build();

        HoldSeatsResponse holdRes = showtimeSeatService.holdSeats(holdReq, userA);

        ReleaseSeatsRequest relReq = ReleaseSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .holdToken(holdRes.getHoldToken())
                .build();

        assertThrows(SeatHoldOwnershipException.class, () -> showtimeSeatService.releaseSeats(relReq, userB));

        ShowtimeSeat dbSeat = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA1.getId()).orElseThrow();
        assertEquals(ShowtimeSeatStatus.HELD, dbSeat.getStatus());
    }

    @Test
    @DisplayName("TEST 12: Release seats with invalid holdToken")
    void testReleaseSeatsInvalidToken() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        Seat seatA1 = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()).get(0);

        HoldSeatsRequest holdReq = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build();

        showtimeSeatService.holdSeats(holdReq, userA);

        ReleaseSeatsRequest relReq = ReleaseSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .holdToken("WRONG-TOKEN")
                .build();

        assertThrows(InvalidSeatHoldException.class, () -> showtimeSeatService.releaseSeats(relReq, userA));
    }

    @Test
    @DisplayName("TEST 13: Cannot hold started showtime")
    void testCannotHoldStartedShowtime() {
        Showtime pastShowtime = showtimeRepository.save(Showtime.builder()
                .movie(testMovie)
                .room(room50)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .priceStandard(new BigDecimal("90000.00"))
                .priceVip(new BigDecimal("95000.00"))
                .priceCouple(new BigDecimal("100000.00"))
                .isActive(true)
                .isOnlineSelling(true)
                .build());


        showtimeSeatService.initializeSeatsForShowtime(pastShowtime);
        Seat seatA1 = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()).get(0);

        HoldSeatsRequest req = HoldSeatsRequest.builder()
                .showtimeId(pastShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build();

        assertThrows(ShowtimeNotBookableException.class, () -> showtimeSeatService.holdSeats(req, userA));
    }

    @Test
    @DisplayName("TEST 14: Cannot hold offline-selling showtime")
    void testCannotHoldOfflineSellingShowtime() {
        futureShowtime.setIsOnlineSelling(false);
        showtimeRepository.save(futureShowtime);

        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        Seat seatA1 = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()).get(0);

        HoldSeatsRequest req = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build();

        assertThrows(ShowtimeNotBookableException.class, () -> showtimeSeatService.holdSeats(req, userA));
    }

    @Test
    @DisplayName("TEST 15: Cannot hold inactive showtime")
    void testCannotHoldInactiveShowtime() {
        futureShowtime.setIsActive(false);
        showtimeRepository.save(futureShowtime);

        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        Seat seatA1 = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()).get(0);

        HoldSeatsRequest req = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build();

        assertThrows(ShowtimeNotBookableException.class, () -> showtimeSeatService.holdSeats(req, userA));
    }

    @Test
    @DisplayName("TEST 16: Concurrency test - two users holding the same seat simultaneously")
    void testConcurrencyHoldSeat() throws Exception {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        Seat seatA1 = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()).get(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Callable<Void> taskUserA = () -> {
            barrier.await();
            try {
                HoldSeatsRequest req = HoldSeatsRequest.builder()
                        .showtimeId(futureShowtime.getId())
                        .seatIds(List.of(seatA1.getId()))
                        .build();
                showtimeSeatService.holdSeats(req, userA);
                successCount.incrementAndGet();
            } catch (SeatAlreadyReservedException e) {
                failureCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };

        Callable<Void> taskUserB = () -> {
            barrier.await();
            try {
                HoldSeatsRequest req = HoldSeatsRequest.builder()
                        .showtimeId(futureShowtime.getId())
                        .seatIds(List.of(seatA1.getId()))
                        .build();
                showtimeSeatService.holdSeats(req, userB);
                successCount.incrementAndGet();
            } catch (SeatAlreadyReservedException e) {
                failureCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };

        executor.invokeAll(List.of(taskUserA, taskUserB));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Exactly ONE succeeds and ONE fails
        assertEquals(1, successCount.get(), "Exactly one user must succeed");
        assertEquals(1, failureCount.get(), "Exactly one user must be rejected");

        ShowtimeSeat dbSeat = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA1.getId()).orElseThrow();
        assertEquals(ShowtimeSeatStatus.HELD, dbSeat.getStatus());
        assertNotNull(dbSeat.getHeldByUser());
    }

    @Test
    @DisplayName("TEST 17: Deadlock prevention with reverse seat ID ordering")
    void testDeadlockPreventionReverseOrder() throws Exception {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        List<Seat> physicalSeats = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId());
        Long id10 = physicalSeats.get(0).getId();
        Long id11 = physicalSeats.get(1).getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        // User A requests [id10, id11]
        Callable<Void> taskUserA = () -> {
            barrier.await();
            try {
                HoldSeatsRequest req = HoldSeatsRequest.builder()
                        .showtimeId(futureShowtime.getId())
                        .seatIds(List.of(id10, id11))
                        .build();
                showtimeSeatService.holdSeats(req, userA);
            } catch (Exception ignored) {}
            return null;
        };

        // User B requests [id11, id10] (Reverse order)
        Callable<Void> taskUserB = () -> {
            barrier.await();
            try {
                HoldSeatsRequest req = HoldSeatsRequest.builder()
                        .showtimeId(futureShowtime.getId())
                        .seatIds(List.of(id11, id10))
                        .build();
                showtimeSeatService.holdSeats(req, userB);
            } catch (Exception ignored) {}
            return null;
        };

        Future<Void> fA = executor.submit(taskUserA);
        Future<Void> fB = executor.submit(taskUserB);

        // Should complete without deadlock within 5 seconds
        assertDoesNotThrow(() -> {
            fA.get(5, TimeUnit.SECONDS);
            fB.get(5, TimeUnit.SECONDS);
        });

        executor.shutdown();
    }

    @Test
    @DisplayName("TEST 18: First seat creates holdToken and second seat reuses holdToken with preserved expiresAt")
    void testHoldTokenReuseAndPreservedExpiresAt() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        List<Seat> physicalSeats = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId());
        Seat seatA1 = physicalSeats.get(0);
        Seat seatA2 = physicalSeats.get(1);

        // Step 1: Hold A1 (first seat, holdToken == null)
        HoldSeatsRequest req1 = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build();
        HoldSeatsResponse res1 = showtimeSeatService.holdSeats(req1, userA);

        assertNotNull(res1.getHoldToken());
        assertNotNull(res1.getExpiresAt());
        String sessionToken = res1.getHoldToken();
        LocalDateTime originalExpiresAt = res1.getExpiresAt();

        // Step 2: Hold A2 (second seat, reusing sessionToken)
        HoldSeatsRequest req2 = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA2.getId()))
                .holdToken(sessionToken)
                .build();
        HoldSeatsResponse res2 = showtimeSeatService.holdSeats(req2, userA);

        // Verify both seats share identical holdToken and original expiresAt
        assertEquals(sessionToken, res2.getHoldToken(), "Second seat must reuse the same holdToken");
        assertEquals(originalExpiresAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), res2.getExpiresAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), "expiresAt must NOT be reset or extended");

        ShowtimeSeat ss1 = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA1.getId()).orElseThrow();
        ShowtimeSeat ss2 = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA2.getId()).orElseThrow();

        assertEquals(ShowtimeSeatStatus.HELD, ss1.getStatus());
        assertEquals(ShowtimeSeatStatus.HELD, ss2.getStatus());
        assertEquals(sessionToken, ss1.getHoldToken());
        assertEquals(sessionToken, ss2.getHoldToken());
        assertEquals(originalExpiresAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), ss1.getLockedUntil().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        assertEquals(originalExpiresAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), ss2.getLockedUntil().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    }

    @Test
    @DisplayName("TEST 19: Releasing one seat keeps remaining seats held under the same holdToken")
    void testReleasingOneSeatKeepsRemainingSeatsHeld() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        List<Seat> physicalSeats = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId());
        Seat seatA1 = physicalSeats.get(0);
        Seat seatA2 = physicalSeats.get(1);

        // Hold A1 + A2
        HoldSeatsResponse res1 = showtimeSeatService.holdSeats(HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build(), userA);

        showtimeSeatService.holdSeats(HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA2.getId()))
                .holdToken(res1.getHoldToken())
                .build(), userA);

        // Release ONLY A1
        showtimeSeatService.releaseSeats(ReleaseSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .holdToken(res1.getHoldToken())
                .build(), userA);

        ShowtimeSeat ss1 = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA1.getId()).orElseThrow();
        ShowtimeSeat ss2 = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA2.getId()).orElseThrow();

        assertEquals(ShowtimeSeatStatus.AVAILABLE, ss1.getStatus(), "Seat A1 must be AVAILABLE after release");
        assertNull(ss1.getHoldToken(), "Seat A1 holdToken must be cleared");

        assertEquals(ShowtimeSeatStatus.HELD, ss2.getStatus(), "Seat A2 must still be HELD");
        assertEquals(res1.getHoldToken(), ss2.getHoldToken(), "Seat A2 must still have the holdToken");
    }

    @Test
    @DisplayName("TEST 20: Expired hold session cannot be reused to hold new seats")
    void testExpiredHoldSessionCannotBeReused() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        List<Seat> physicalSeats = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId());
        Seat seatA1 = physicalSeats.get(0);
        Seat seatA2 = physicalSeats.get(1);

        // Hold A1
        HoldSeatsResponse res1 = showtimeSeatService.holdSeats(HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build(), userA);

        // Manually expire A1 in DB
        ShowtimeSeat ss1 = showtimeSeatRepository.findByShowtimeIdAndSeatId(futureShowtime.getId(), seatA1.getId()).orElseThrow();
        ss1.setLockedUntil(LocalDateTime.now().minusMinutes(5));
        showtimeSeatRepository.save(ss1);

        // Attempt to hold A2 using expired holdToken
        HoldSeatsRequest req2 = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA2.getId()))
                .holdToken(res1.getHoldToken())
                .build();

        assertThrows(InvalidSeatHoldException.class, () -> {
            showtimeSeatService.holdSeats(req2, userA);
        }, "Must reject reusing an expired hold session");
    }

    @Test
    @DisplayName("TEST 21: Cannot reuse another user's holdToken")
    void testCannotReuseAnotherUserHoldToken() {
        showtimeSeatService.initializeSeatsForShowtime(futureShowtime);
        List<Seat> physicalSeats = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId());
        Seat seatA1 = physicalSeats.get(0);
        Seat seatA2 = physicalSeats.get(1);

        // User A holds A1
        HoldSeatsResponse resA = showtimeSeatService.holdSeats(HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA1.getId()))
                .build(), userA);

        // User B tries to use User A's holdToken
        HoldSeatsRequest reqB = HoldSeatsRequest.builder()
                .showtimeId(futureShowtime.getId())
                .seatIds(List.of(seatA2.getId()))
                .holdToken(resA.getHoldToken())
                .build();

        assertThrows(SeatHoldOwnershipException.class, () -> {
            showtimeSeatService.holdSeats(reqB, userB);
        }, "User B must NOT be able to reuse User A's holdToken");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        orderItemRepository.deleteAll();
        paymentRepository.deleteAll();
        reservedSeatRepository.deleteAll();
        reservationRepository.deleteAll();
        showtimeSeatRepository.deleteAll();
        showtimeRepository.deleteAll();

        if (room50 != null && room50.getId() != null) {
            seatRepository.deleteAll(seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(room50.getId()));
            roomRepository.deleteById(room50.getId());
        }
        if (testMovie != null && testMovie.getId() != null) {
            movieRepository.deleteById(testMovie.getId());
        }
        if (userA != null && userA.getId() != null) {
            userRepository.deleteById(userA.getId());
        }
        if (userB != null && userB.getId() != null) {
            userRepository.deleteById(userB.getId());
        }
    }
}

