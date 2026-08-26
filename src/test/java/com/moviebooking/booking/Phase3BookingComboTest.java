package com.moviebooking.booking;

import com.moviebooking.dto.req.AddComboRequest;
import com.moviebooking.dto.req.UpdateComboQuantityRequest;
import com.moviebooking.dto.res.ReservationReviewResponse;
import com.moviebooking.exception.*;
import com.moviebooking.model.*;
import com.moviebooking.model.enums.*;
import com.moviebooking.repository.*;
import com.moviebooking.service.booking.BookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
public class Phase3BookingComboTest {


    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ReservedSeatRepository reservedSeatRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowtimeSeatRepository showtimeSeatRepository;

    private User userA;
    private User userB;
    private Product foodProduct;
    private Product drinkProduct;
    private Product comboProduct;
    private Product inactiveProduct;
    private Product lowStockProduct;
    private Reservation pendingReservation;
    private ShowtimeSeat testShowtimeSeat;
    private Seat seatA1;
    private Room roomTest;
    private Movie movieTest;
    private Showtime showtimeTest;

    @BeforeEach
    void setUp() {
        userA = userRepository.findByEmail("user_a_p3@example.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("user_a_p3@example.com")
                        .password("password")
                        .firstName("User")
                        .lastName("A")
                        .phone("0911000001")
                        .dateOfBirth(LocalDate.of(2000, 1, 1))
                        .gender(Gender.MALE)
                        .role(Role.USER)
                        .build())
        );

        userB = userRepository.findByEmail("user_b_p3@example.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("user_b_p3@example.com")
                        .password("password")
                        .firstName("User")
                        .lastName("B")
                        .phone("0911000002")
                        .dateOfBirth(LocalDate.of(2000, 1, 1))
                        .gender(Gender.FEMALE)
                        .role(Role.USER)
                        .build())
        );

        foodProduct = productRepository.save(Product.builder()
                .name("Popcorn Caramel Phase 3")
                .category(ProductCategory.FOOD)
                .description("Bắp rang bơ vị caramel")
                .price(new BigDecimal("50000.00"))
                .availableQuantity(100)
                .imagePath("/products/popcorn.jpg")
                .isActive(true)
                .displayOrder(1)
                .build());

        drinkProduct = productRepository.save(Product.builder()
                .name("Coca Cola Phase 3")
                .category(ProductCategory.DRINK)
                .description("Nước ngọt Coca Cola 500ml")
                .price(new BigDecimal("30000.00"))
                .availableQuantity(50)
                .imagePath("/products/coca.jpg")
                .isActive(true)
                .displayOrder(2)
                .build());

        comboProduct = productRepository.save(Product.builder()
                .name("Combo Single Phase 3")
                .category(ProductCategory.COMBO)
                .description("1 Bắp + 1 Nước")
                .price(new BigDecimal("70000.00"))
                .availableQuantity(20)
                .imagePath("/products/combo1.jpg")
                .isActive(true)
                .displayOrder(3)
                .build());

        inactiveProduct = productRepository.save(Product.builder()
                .name("Inactive Drink Phase 3")
                .category(ProductCategory.DRINK)
                .description("Sản phẩm ngưng bán")
                .price(new BigDecimal("25000.00"))
                .availableQuantity(50)
                .imagePath("/products/inactive.jpg")
                .isActive(false)
                .displayOrder(4)
                .build());

        lowStockProduct = productRepository.save(Product.builder()
                .name("Limited Item Phase 3")
                .category(ProductCategory.FOOD)
                .description("Sản phẩm giới hạn")
                .price(new BigDecimal("40000.00"))
                .availableQuantity(3)
                .imagePath("/products/limited.jpg")
                .isActive(true)
                .displayOrder(5)
                .build());

        Theater theater = theaterRepository.findAll().stream().findFirst().orElseGet(() ->
                theaterRepository.save(Theater.builder()
                        .name("CineMind Phase 3 Theater")
                        .address("123 Test Street")
                        .city("Hà Nội")
                        .district("Cau Giay")
                        .email("p3@cinemind.vn")
                        .phone("0249999888")
                        .isActive(true)
                        .build())
        );

        roomTest = roomRepository.save(Room.builder()
                .name("Phòng chiếu Test Phase 3")
                .roomType(RoomType.TWO_D)
                .theater(theater)
                .isActive(true)
                .build());


        seatA1 = seatRepository.save(Seat.builder()
                .room(roomTest)
                .rowName("A")
                .seatNumber(1)
                .seatType(SeatType.STANDARD)
                .isActive(true)
                .build());

        movieTest = movieRepository.save(Movie.builder()
                .title("Test Movie Phase 3")
                .description("Mô tả phim test Phase 3")
                .duration(120)
                .director("Director Test")
                .actors("Actor 1, Actor 2")
                .language("Tiếng Việt")
                .posterPath("/posters/test.jpg")
                .bannerPath("/banners/test.jpg")
                .ageRating(AgeRating.P)
                .status(MovieStatus.NOW_SHOWING)
                .releaseDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .build());

        showtimeTest = showtimeRepository.save(Showtime.builder()
                .movie(movieTest)
                .room(roomTest)
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusHours(4))
                .priceStandard(new BigDecimal("90000.00"))
                .priceVip(new BigDecimal("100000.00"))
                .priceCouple(new BigDecimal("120000.00"))
                .isActive(true)
                .build());

        testShowtimeSeat = showtimeSeatRepository.save(ShowtimeSeat.builder()
                .showtime(showtimeTest)
                .seat(seatA1)
                .status(ShowtimeSeatStatus.HELD)
                .holdToken(UUID.randomUUID().toString())
                .heldByUser(userA)
                .lockedUntil(LocalDateTime.now().plusMinutes(8))
                .price(new BigDecimal("90000.00"))
                .build());


        pendingReservation = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-P3-TEST")
                .user(userA)
                .showtime(showtimeTest)
                .totalPrice(new BigDecimal("90000.00"))
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(8))
                .build());

        reservedSeatRepository.save(ReservedSeat.builder()
                .reservation(pendingReservation)
                .seat(seatA1)
                .price(new BigDecimal("90000.00"))
                .build());
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @DisplayName("TEST 1: Add active FOOD successfully")
    void testAddActiveFood() {
        AddComboRequest req = AddComboRequest.builder()
                .productId(foodProduct.getId())
                .quantity(2)
                .build();

        ReservationReviewResponse res = bookingService.addComboToReservation(pendingReservation.getId(), req, userA);
        assertNotNull(res);
        assertEquals(1, res.getItems().size());
        assertEquals("Popcorn Caramel Phase 3", res.getItems().get(0).getProductName());
        assertEquals(new BigDecimal("100000.00"), res.getFnbSubtotal());
        assertEquals(new BigDecimal("190000.00"), res.getTotalAmount());
    }

    @Test
    @DisplayName("TEST 2: Add active DRINK successfully")
    void testAddActiveDrink() {
        AddComboRequest req = AddComboRequest.builder()
                .productId(drinkProduct.getId())
                .quantity(3)
                .build();

        ReservationReviewResponse res = bookingService.addComboToReservation(pendingReservation.getId(), req, userA);
        assertEquals(1, res.getItems().size());
        assertEquals(new BigDecimal("90000.00"), res.getFnbSubtotal());
        assertEquals(new BigDecimal("180000.00"), res.getTotalAmount());
    }

    @Test
    @DisplayName("TEST 3: Add active COMBO successfully")
    void testAddActiveCombo() {
        AddComboRequest req = AddComboRequest.builder()
                .productId(comboProduct.getId())
                .quantity(1)
                .build();

        ReservationReviewResponse res = bookingService.addComboToReservation(pendingReservation.getId(), req, userA);
        assertEquals(1, res.getItems().size());
        assertEquals(new BigDecimal("70000.00"), res.getFnbSubtotal());
        assertEquals(new BigDecimal("160000.00"), res.getTotalAmount());
    }

    @Test
    @DisplayName("TEST 4: Merge duplicate product in same reservation")
    void testMergeDuplicateProduct() {
        AddComboRequest req1 = AddComboRequest.builder().productId(foodProduct.getId()).quantity(2).build();
        bookingService.addComboToReservation(pendingReservation.getId(), req1, userA);

        AddComboRequest req2 = AddComboRequest.builder().productId(foodProduct.getId()).quantity(3).build();
        ReservationReviewResponse res = bookingService.addComboToReservation(pendingReservation.getId(), req2, userA);

        assertEquals(1, res.getItems().size(), "Should merge into ONE OrderItem");
        assertEquals(5, res.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("50000.00"), res.getItems().get(0).getUnitPrice());
        assertEquals(new BigDecimal("250000.00"), res.getItems().get(0).getSubtotal());
    }

    @Test
    @DisplayName("TEST 5: Quantity zero rejection")
    void testQuantityZeroRejection() {
        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(0).build();
        assertThrows(InvalidQuantityException.class, () -> bookingService.addComboToReservation(pendingReservation.getId(), req, userA));
    }

    @Test
    @DisplayName("TEST 6: Quantity negative rejection")
    void testQuantityNegativeRejection() {
        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(-5).build();
        assertThrows(InvalidQuantityException.class, () -> bookingService.addComboToReservation(pendingReservation.getId(), req, userA));
    }

    @Test
    @DisplayName("TEST 7: Product not found rejection")
    void testProductNotFoundRejection() {
        AddComboRequest req = AddComboRequest.builder().productId(999999L).quantity(1).build();
        assertThrows(ResourceNotFoundException.class, () -> bookingService.addComboToReservation(pendingReservation.getId(), req, userA));
    }

    @Test
    @DisplayName("TEST 8: Inactive product rejection")
    void testInactiveProductRejection() {
        AddComboRequest req = AddComboRequest.builder().productId(inactiveProduct.getId()).quantity(1).build();
        assertThrows(ProductInactiveException.class, () -> bookingService.addComboToReservation(pendingReservation.getId(), req, userA));
    }

    @Test
    @DisplayName("TEST 9: Insufficient inventory soft check rejection")
    void testInsufficientInventoryRejection() {
        AddComboRequest req = AddComboRequest.builder().productId(lowStockProduct.getId()).quantity(5).build();
        assertThrows(InsufficientInventoryException.class, () -> bookingService.addComboToReservation(pendingReservation.getId(), req, userA));
    }

    @Test
    @DisplayName("TEST 10: Ownership violation rejection")
    void testOwnershipViolationRejection() {
        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(1).build();
        assertThrows(SeatHoldOwnershipException.class, () -> bookingService.addComboToReservation(pendingReservation.getId(), req, userB));
    }

    @Test
    @DisplayName("TEST 11: Price snapshot at addition moment")
    void testPriceSnapshotAtAddition() {
        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(2).build();
        ReservationReviewResponse res = bookingService.addComboToReservation(pendingReservation.getId(), req, userA);

        assertEquals(new BigDecimal("50000.00"), res.getItems().get(0).getUnitPrice());
    }

    @Test
    @DisplayName("TEST 12: Product price changed after adding -> Old OrderItem price unchanged")
    void testProductPriceChangedAfterAdding() {
        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(2).build();
        bookingService.addComboToReservation(pendingReservation.getId(), req, userA);

        // Admin changes product price to 80000.00
        foodProduct.setPrice(new BigDecimal("80000.00"));
        productRepository.save(foodProduct);

        ReservationReviewResponse res = bookingService.reviewReservation(pendingReservation.getId(), userA);
        assertEquals(new BigDecimal("50000.00"), res.getItems().get(0).getUnitPrice(), "Price snapshot must remain 50000.00");
        assertEquals(new BigDecimal("100000.00"), res.getItems().get(0).getSubtotal());
    }

    @Test
    @DisplayName("TEST 13: Update quantity")
    void testUpdateQuantity() {
        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(2).build();
        ReservationReviewResponse res1 = bookingService.addComboToReservation(pendingReservation.getId(), req, userA);
        Long itemId = res1.getItems().get(0).getItemId();

        UpdateComboQuantityRequest updateReq = UpdateComboQuantityRequest.builder().quantity(4).build();
        ReservationReviewResponse res2 = bookingService.updateComboQuantity(pendingReservation.getId(), itemId, updateReq, userA);

        assertEquals(4, res2.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("200000.00"), res2.getItems().get(0).getSubtotal());
    }

    @Test
    @DisplayName("TEST 14: Delete item")
    void testDeleteItem() {
        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(2).build();
        ReservationReviewResponse res1 = bookingService.addComboToReservation(pendingReservation.getId(), req, userA);
        Long itemId = res1.getItems().get(0).getItemId();

        ReservationReviewResponse res2 = bookingService.removeComboFromReservation(pendingReservation.getId(), itemId, userA);
        assertTrue(res2.getItems().isEmpty());
        assertEquals(new BigDecimal("90000.00"), res2.getTotalAmount());
    }

    @Test
    @DisplayName("TEST 15: Total calculation using BigDecimal")
    void testTotalCalculation() {
        AddComboRequest req1 = AddComboRequest.builder().productId(foodProduct.getId()).quantity(2).build(); // 100k
        AddComboRequest req2 = AddComboRequest.builder().productId(drinkProduct.getId()).quantity(1).build(); // 30k
        bookingService.addComboToReservation(pendingReservation.getId(), req1, userA);
        ReservationReviewResponse res = bookingService.addComboToReservation(pendingReservation.getId(), req2, userA);

        assertEquals(new BigDecimal("90000.00"), res.getTicketSubtotal());
        assertEquals(new BigDecimal("130000.00"), res.getFnbSubtotal());
        assertEquals(new BigDecimal("220000.00"), res.getTotalAmount());
    }

    @Test
    @DisplayName("TEST 16: Payment FAILED + valid reservation -> Modifiable")
    void testPaymentFailedReservationValidModifiable() {
        paymentRepository.save(Payment.builder()
                .reservation(pendingReservation)
                .amount(new BigDecimal("90000.00"))
                .status(PaymentStatus.FAILED)
                .build());

        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(1).build();
        assertDoesNotThrow(() -> bookingService.addComboToReservation(pendingReservation.getId(), req, userA));
    }

    @Test
    @DisplayName("TEST 17: Payment COMPLETED -> Immutability rejection")
    void testPaymentCompletedImmutability() {
        paymentRepository.save(Payment.builder()
                .reservation(pendingReservation)
                .amount(new BigDecimal("90000.00"))
                .status(PaymentStatus.COMPLETED)
                .build());

        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(1).build();
        assertThrows(ReservationNotModifiableException.class, () -> bookingService.addComboToReservation(pendingReservation.getId(), req, userA));
    }

    @Test
    @DisplayName("TEST 18: EXPIRED reservation -> Rejection")
    void testExpiredReservationImmutability() {
        pendingReservation.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(pendingReservation);

        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(1).build();
        assertThrows(ReservationNotModifiableException.class, () -> bookingService.addComboToReservation(pendingReservation.getId(), req, userA));
    }

    @Test
    @DisplayName("TEST 19: CANCELLED reservation -> Rejection")
    void testCancelledReservationImmutability() {
        pendingReservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(pendingReservation);

        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(1).build();
        assertThrows(ReservationNotModifiableException.class, () -> bookingService.addComboToReservation(pendingReservation.getId(), req, userA));
    }

    @Test
    @DisplayName("TEST 20: Phase 2 ShowtimeSeat unchanged by F&B operations")
    void testPhase2ShowtimeSeatUnchanged() {
        ShowtimeSeat seatBefore = showtimeSeatRepository.findById(testShowtimeSeat.getId()).orElseThrow();
        String holdTokenBefore = seatBefore.getHoldToken();
        ShowtimeSeatStatus statusBefore = seatBefore.getStatus();

        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(2).build();
        bookingService.addComboToReservation(pendingReservation.getId(), req, userA);

        ShowtimeSeat seatAfter = showtimeSeatRepository.findById(testShowtimeSeat.getId()).orElseThrow();
        assertEquals(statusBefore, seatAfter.getStatus());
        assertEquals(holdTokenBefore, seatAfter.getHoldToken());
        assertEquals(userA.getId(), seatAfter.getHeldByUser().getId());
    }

    @Test
    @DisplayName("TEST 21: Transaction rollback on validation failure")
    void testTransactionRollbackOnValidationFailure() {
        AddComboRequest validReq = AddComboRequest.builder().productId(foodProduct.getId()).quantity(1).build();
        bookingService.addComboToReservation(pendingReservation.getId(), validReq, userA);

        AddComboRequest invalidReq = AddComboRequest.builder().productId(inactiveProduct.getId()).quantity(1).build();
        assertThrows(ProductInactiveException.class, () -> bookingService.addComboToReservation(pendingReservation.getId(), invalidReq, userA));

        ReservationReviewResponse res = bookingService.reviewReservation(pendingReservation.getId(), userA);
        assertEquals(1, res.getItems().size());
        assertEquals(new BigDecimal("140000.00"), res.getTotalAmount());
    }

    @Test
    @DisplayName("TEST 22: Review order response format")
    void testReviewOrderResponseFormat() {
        AddComboRequest req = AddComboRequest.builder().productId(foodProduct.getId()).quantity(2).build();
        bookingService.addComboToReservation(pendingReservation.getId(), req, userA);

        ReservationReviewResponse res = bookingService.reviewReservation(pendingReservation.getId(), userA);
        assertNotNull(res.getBookingCode());
        assertEquals("Test Movie Phase 3", res.getMovieTitle());
        assertNotNull(res.getShowtimeStart());
        assertEquals(1, res.getTicketSeats().size());
        assertEquals("A", res.getTicketSeats().get(0).getRowName());
        assertEquals(1, res.getTicketSeats().get(0).getSeatNumber());
        assertEquals(new BigDecimal("90000.00"), res.getTicketSubtotal());
        assertEquals(new BigDecimal("100000.00"), res.getFnbSubtotal());
        assertEquals(new BigDecimal("190000.00"), res.getTotalAmount());
    }
}
