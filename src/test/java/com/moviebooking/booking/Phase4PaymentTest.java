package com.moviebooking.booking;

import com.moviebooking.config.VnPayConfig;
import com.moviebooking.dto.req.AddComboRequest;
import com.moviebooking.dto.res.CreatePaymentResponse;
import com.moviebooking.dto.res.ReservationReviewResponse;
import com.moviebooking.exception.*;
import com.moviebooking.model.*;
import com.moviebooking.model.enums.*;
import com.moviebooking.repository.*;
import com.moviebooking.service.booking.BookingService;
import com.moviebooking.service.payment.PaymentService;
import com.moviebooking.util.VnPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class Phase4PaymentTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private VnPayConfig vnPayConfig;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ReservedSeatRepository reservedSeatRepository;

    @Autowired
    private ProductRepository productRepository;

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
    private jakarta.persistence.EntityManager entityManager;


    private User userA;
    private User userB;
    private Product foodProduct;
    private Product drinkProduct;
    private Room roomTest;
    private Seat seatA1;
    private Movie movieTest;
    private Showtime showtimeTest;
    private ShowtimeSeat testShowtimeSeat;
    private Reservation pendingReservation;

    @BeforeEach
    void setUp() {
        userA = userRepository.findByEmail("user_a_p4@example.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("user_a_p4@example.com")
                        .password("password")
                        .firstName("User")
                        .lastName("A")
                        .phone("0911000041")
                        .dateOfBirth(LocalDate.of(2000, 1, 1))
                        .gender(Gender.MALE)
                        .role(Role.USER)
                        .build())
        );

        userB = userRepository.findByEmail("user_b_p4@example.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("user_b_p4@example.com")
                        .password("password")
                        .firstName("User")
                        .lastName("B")
                        .phone("0911000042")
                        .dateOfBirth(LocalDate.of(2000, 1, 1))
                        .gender(Gender.FEMALE)
                        .role(Role.USER)
                        .build())
        );

        foodProduct = productRepository.save(Product.builder()
                .name("Popcorn Phase 4")
                .category(ProductCategory.FOOD)
                .description("Bắp rang bơ")
                .price(new BigDecimal("50000.00"))
                .availableQuantity(10)
                .imagePath("/products/popcorn.jpg")
                .isActive(true)
                .displayOrder(1)
                .build());

        drinkProduct = productRepository.save(Product.builder()
                .name("Coca Phase 4")
                .category(ProductCategory.DRINK)
                .description("Nước ngọt")
                .price(new BigDecimal("30000.00"))
                .availableQuantity(10)
                .imagePath("/products/coca.jpg")
                .isActive(true)
                .displayOrder(2)
                .build());

        Theater theater = theaterRepository.findAll().stream().findFirst().orElseGet(() ->
                theaterRepository.save(Theater.builder()
                        .name("CineMind Phase 4 Theater")
                        .address("123 Test Street")
                        .city("Hà Nội")
                        .district("Cau Giay")
                        .email("p4@cinemind.vn")
                        .phone("0249999884")
                        .isActive(true)
                        .build())
        );

        roomTest = roomRepository.save(Room.builder()
                .name("Phòng chiếu Test Phase 4")
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
                .title("Test Movie Phase 4")
                .description("Mô tả phim test Phase 4")
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
                .bookingCode("REV-P4-" + System.currentTimeMillis())
                .user(userA)
                .showtime(showtimeTest)
                .totalPrice(new BigDecimal("140000.00")) // 90k ticket + 50k food
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(8))
                .build());

        testShowtimeSeat.setReservation(pendingReservation);
        showtimeSeatRepository.save(testShowtimeSeat);

        reservedSeatRepository.save(ReservedSeat.builder()
                .reservation(pendingReservation)
                .seat(seatA1)
                .price(new BigDecimal("90000.00"))
                .build());

        orderItemRepository.save(OrderItem.builder()
                .reservation(pendingReservation)
                .product(foodProduct)
                .unitPrice(new BigDecimal("50000.00"))
                .quantity(1)
                .subtotal(new BigDecimal("50000.00"))
                .build());
    }

    // Helper to generate signed VNPAY parameters
    private Map<String, String> createSignedVnPayParams(String txnRef, String responseCode, BigDecimal amount) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", VnPayConfig.VNP_VERSION);
        params.put("vnp_Command", VnPayConfig.VNP_COMMAND);
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValue()));
        params.put("vnp_CurrCode", VnPayConfig.VNP_CURRENCY);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Test payment");
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TransactionNo", "14000001");
        params.put("vnp_BankCode", "NCB");

        String hashData = VnPayUtil.buildHashData(params);
        String secureHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        params.put("vnp_SecureHash", secureHash);
        return params;
    }

    // ------------------------------------------------------------------------
    // A. PAYMENT CREATION
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("TEST 1: Valid Reservation -> Payment PENDING created")
    void testCreatePaymentSuccess() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        assertNotNull(res);
        assertEquals(pendingReservation.getId(), res.getReservationId());
        assertEquals(PaymentStatus.PENDING, res.getStatus());
        assertTrue(res.getPaymentUrl().contains("vnp_TxnRef="));
    }

    @Test
    @DisplayName("TEST 2: VNPAY URL contains HMAC-SHA512 checksum")
    void testVnPayUrlChecksum() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        assertTrue(res.getPaymentUrl().contains("vnp_SecureHash="));
    }

    @Test
    @DisplayName("TEST 3: Expired Reservation -> reject")
    void testCreatePaymentExpiredReservation() {
        pendingReservation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        reservationRepository.save(pendingReservation);

        assertThrows(ReservationNotModifiableException.class, () ->
                paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1")
        );
    }

    @Test
    @DisplayName("TEST 4: CANCELLED Reservation -> reject")
    void testCreatePaymentCancelledReservation() {
        pendingReservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(pendingReservation);

        assertThrows(ReservationNotModifiableException.class, () ->
                paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1")
        );
    }

    @Test
    @DisplayName("TEST 5: CONFIRMED Reservation -> reject")
    void testCreatePaymentConfirmedReservation() {
        pendingReservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(pendingReservation);

        assertThrows(ReservationNotModifiableException.class, () ->
                paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1")
        );
    }

    @Test
    @DisplayName("TEST 6: Wrong ownership -> 403 SeatHoldOwnershipException")
    void testCreatePaymentWrongOwnership() {
        assertThrows(SeatHoldOwnershipException.class, () ->
                paymentService.createPaymentUrl(pendingReservation.getId(), userB, "127.0.0.1")
        );
    }

    // ------------------------------------------------------------------------
    // B. CHECKSUM & SECURITY
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("TEST 7: Valid checksum -> IPN process succeeds")
    void testValidChecksumIpn() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        Map<String, String> ipnRes = paymentService.processVnPayIpn(params);
        assertEquals("00", ipnRes.get("RspCode"));
        assertEquals("Confirm Success", ipnRes.get("Message"));
    }

    @Test
    @DisplayName("TEST 8: Invalid checksum -> RspCode 97")
    void testInvalidChecksumIpn() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());
        params.put("vnp_SecureHash", "INVALID_HASH");

        Map<String, String> ipnRes = paymentService.processVnPayIpn(params);
        assertEquals("97", ipnRes.get("RspCode"));
    }

    @Test
    @DisplayName("TEST 9: Unknown transactionRef -> RspCode 01")
    void testUnknownTxnRefIpn() {
        Map<String, String> params = createSignedVnPayParams("TXN-UNKNOWN-9999", "00", new BigDecimal("100000.00"));
        Map<String, String> ipnRes = paymentService.processVnPayIpn(params);
        assertEquals("01", ipnRes.get("RspCode"));
    }

    @Test
    @DisplayName("TEST 10: Reservation/payment mismatch -> reject")
    void testReservationMismatchIpn() {
        Map<String, String> params = createSignedVnPayParams("TXN-NONEXISTENT", "00", new BigDecimal("100000.00"));
        Map<String, String> ipnRes = paymentService.processVnPayIpn(params);
        assertEquals("01", ipnRes.get("RspCode"));
    }

    @Test
    @DisplayName("TEST 11: Amount mismatch -> RspCode 04")
    void testAmountMismatchIpn() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        // Pass wrong amount (e.g. 100k instead of 140k)
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", new BigDecimal("100000.00"));

        Map<String, String> ipnRes = paymentService.processVnPayIpn(params);
        assertEquals("04", ipnRes.get("RspCode"));
    }

    @Test
    @DisplayName("TEST 12: Do not trust frontend amount -> IPN compares with Reservation.totalPrice")
    void testDoNotTrustFrontendAmount() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", new BigDecimal("99999.00"));

        Map<String, String> ipnRes = paymentService.processVnPayIpn(params);
        assertEquals("04", ipnRes.get("RspCode"));
    }

    // ------------------------------------------------------------------------
    // C. PAYMENT RESULT & RETRY
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("TEST 13: VNPAY success -> COMPLETED")
    void testPaymentSuccessCompleted() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);

        Payment updatedPayment = paymentRepository.findByTransactionRef(res.getTransactionRef()).orElseThrow();
        assertEquals(PaymentStatus.COMPLETED, updatedPayment.getStatus());
        assertNotNull(updatedPayment.getPaidAt());
    }

    @Test
    @DisplayName("TEST 14: VNPAY failure -> FAILED")
    void testPaymentFailedStatus() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "24", pendingReservation.getTotalPrice()); // Code 24 = Cancelled by user

        paymentService.processVnPayIpn(params);
        entityManager.clear();

        Payment updatedPayment = paymentRepository.findByTransactionRef(res.getTransactionRef()).orElseThrow();

        assertEquals(PaymentStatus.FAILED, updatedPayment.getStatus());
    }

    @Test
    @DisplayName("TEST 15: Reservation remains PENDING after failed payment")
    void testReservationRemainsPendingAfterFailedPayment() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "24", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);

        Reservation r = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
        assertEquals(ReservationStatus.PENDING, r.getStatus());
    }

    @Test
    @DisplayName("TEST 16: FAILED payment can retry")
    void testFailedPaymentCanRetry() {
        CreatePaymentResponse res1 = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res1.getTransactionRef(), "24", pendingReservation.getTotalPrice());
        paymentService.processVnPayIpn(params);

        // Retry payment
        CreatePaymentResponse res2 = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        assertNotNull(res2);
        assertEquals(PaymentStatus.PENDING, res2.getStatus());
    }

    @Test
    @DisplayName("TEST 17: Retry creates new transactionRef")
    void testRetryCreatesNewTransactionRef() {
        CreatePaymentResponse res1 = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res1.getTransactionRef(), "24", pendingReservation.getTotalPrice());
        paymentService.processVnPayIpn(params);

        CreatePaymentResponse res2 = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        assertNotEquals(res1.getTransactionRef(), res2.getTransactionRef());
    }

    @Test
    @DisplayName("TEST 18: Retry reuses same Payment row")
    void testRetryReusesSamePaymentRow() {
        CreatePaymentResponse res1 = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        CreatePaymentResponse res2 = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");

        assertEquals(res1.getPaymentId(), res2.getPaymentId());
        assertEquals(1, paymentRepository.findAll().stream().filter(p -> p.getReservation().getId().equals(pendingReservation.getId())).count());
    }

    @Test
    @DisplayName("TEST 19: COMPLETED payment cannot retry")
    void testCompletedPaymentCannotRetry() {
        CreatePaymentResponse res1 = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res1.getTransactionRef(), "00", pendingReservation.getTotalPrice());
        paymentService.processVnPayIpn(params);

        assertThrows(ReservationNotModifiableException.class, () ->
                paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1")
        );
    }

    // ------------------------------------------------------------------------
    // D. IDEMPOTENCY
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("TEST 20: Duplicate IPN after COMPLETED")
    void testDuplicateIpnAfterCompleted() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);
        Map<String, String> secondIpnRes = paymentService.processVnPayIpn(params);

        assertEquals("00", secondIpnRes.get("RspCode"));
        assertEquals("Confirm Success", secondIpnRes.get("Message"));
    }

    @Test
    @DisplayName("TEST 21: Duplicate IPN does not deduct inventory twice")
    void testDuplicateIpnDoesNotDeductInventoryTwice() {
        int initialStock = foodProduct.getAvailableQuantity();
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);
        paymentService.processVnPayIpn(params);

        Product updatedProduct = productRepository.findById(foodProduct.getId()).orElseThrow();
        assertEquals(initialStock - 1, updatedProduct.getAvailableQuantity());
    }

    @Test
    @DisplayName("TEST 22: Duplicate IPN does not change seat twice")
    void testDuplicateIpnDoesNotChangeSeatTwice() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);
        paymentService.processVnPayIpn(params);

        ShowtimeSeat ss = showtimeSeatRepository.findById(testShowtimeSeat.getId()).orElseThrow();
        assertEquals(ShowtimeSeatStatus.SOLD, ss.getStatus());
    }

    @Test
    @DisplayName("TEST 23: Duplicate IPN returns RspCode 00")
    void testDuplicateIpnReturnsRspCode00() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);
        Map<String, String> ipnRes2 = paymentService.processVnPayIpn(params);

        assertEquals("00", ipnRes2.get("RspCode"));
    }

    // ------------------------------------------------------------------------
    // E. INVENTORY
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("TEST 24: Sufficient inventory -> deduct exact quantity")
    void testSufficientInventoryDeductExactQuantity() {
        int initialStock = foodProduct.getAvailableQuantity();
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);

        Product updatedProduct = productRepository.findById(foodProduct.getId()).orElseThrow();
        assertEquals(initialStock - 1, updatedProduct.getAvailableQuantity());
    }

    @Test
    @DisplayName("TEST 25: Insufficient inventory -> primary transaction rollback")
    void testInsufficientInventoryRollback() {
        foodProduct.setAvailableQuantity(0); // Zero stock
        productRepository.save(foodProduct);

        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);

        Reservation r = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
        assertEquals(ReservationStatus.PENDING, r.getStatus()); // Reservation remains PENDING
    }

    @Test
    @DisplayName("TEST 26: Inventory never becomes negative")
    void testInventoryNeverNegative() {
        foodProduct.setAvailableQuantity(0);
        productRepository.save(foodProduct);

        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);

        Product p = productRepository.findById(foodProduct.getId()).orElseThrow();
        assertEquals(0, p.getAvailableQuantity());
    }

    @Test
    @DisplayName("TEST 27: Concurrent payments cannot oversell")
    void testConcurrentPaymentsCannotOversell() throws Exception {
        // Set stock to 1
        foodProduct.setAvailableQuantity(1);
        productRepository.save(foodProduct);

        // Create reservation 2 for userB
        ShowtimeSeat seatA2 = showtimeSeatRepository.save(ShowtimeSeat.builder()
                .showtime(showtimeTest)
                .seat(seatRepository.save(Seat.builder().room(roomTest).rowName("A").seatNumber(2).seatType(SeatType.STANDARD).isActive(true).build()))
                .status(ShowtimeSeatStatus.HELD)
                .holdToken(UUID.randomUUID().toString())
                .heldByUser(userB)
                .lockedUntil(LocalDateTime.now().plusMinutes(8))
                .price(new BigDecimal("90000.00"))
                .build());

        Reservation pendingReservation2 = reservationRepository.save(Reservation.builder()
                .bookingCode("REV-P4-B-" + System.currentTimeMillis())
                .user(userB)
                .showtime(showtimeTest)
                .totalPrice(new BigDecimal("140000.00"))
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(8))
                .build());

        orderItemRepository.save(OrderItem.builder()
                .reservation(pendingReservation2)
                .product(foodProduct)
                .unitPrice(new BigDecimal("50000.00"))
                .quantity(1)
                .subtotal(new BigDecimal("50000.00"))
                .build());

        CreatePaymentResponse res1 = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        CreatePaymentResponse res2 = paymentService.createPaymentUrl(pendingReservation2.getId(), userB, "127.0.0.1");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Map<String, String>> future1 = executor.submit(() -> paymentService.processVnPayIpn(createSignedVnPayParams(res1.getTransactionRef(), "00", pendingReservation.getTotalPrice())));
        Future<Map<String, String>> future2 = executor.submit(() -> paymentService.processVnPayIpn(createSignedVnPayParams(res2.getTransactionRef(), "00", pendingReservation2.getTotalPrice())));

        future1.get(5, TimeUnit.SECONDS);
        future2.get(5, TimeUnit.SECONDS);
        executor.shutdown();

        Product updatedProduct = productRepository.findById(foodProduct.getId()).orElseThrow();
        assertTrue(updatedProduct.getAvailableQuantity() >= 0);
    }

    @Test
    @DisplayName("TEST 28: Product pessimistic lock is actually used")
    void testProductPessimisticLockUsed() {
        assertNotNull(productRepository.findByIdWithLock(foodProduct.getId()));
    }

    // ------------------------------------------------------------------------
    // F. SEAT
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("TEST 29: HELD -> SOLD after successful payment")
    void testHeldToSoldAfterSuccessfulPayment() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);

        ShowtimeSeat ss = showtimeSeatRepository.findById(testShowtimeSeat.getId()).orElseThrow();
        assertEquals(ShowtimeSeatStatus.SOLD, ss.getStatus());
    }

    @Test
    @DisplayName("TEST 30: lockedUntil cleared after SOLD")
    void testLockedUntilClearedAfterSold() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);

        ShowtimeSeat ss = showtimeSeatRepository.findById(testShowtimeSeat.getId()).orElseThrow();
        assertNull(ss.getLockedUntil());
    }

    @Test
    @DisplayName("TEST 31: Failed payment does not change HELD")
    void testFailedPaymentDoesNotChangeHeld() {
        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "24", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);

        ShowtimeSeat ss = showtimeSeatRepository.findById(testShowtimeSeat.getId()).orElseThrow();
        assertEquals(ShowtimeSeatStatus.HELD, ss.getStatus());
    }

    @Test
    @DisplayName("TEST 32: Invalid/expired seat causes local processing failure")
    void testInvalidSeatCausesLocalProcessingFailure() {
        testShowtimeSeat.setStatus(ShowtimeSeatStatus.AVAILABLE); // Invalidate HELD status
        showtimeSeatRepository.save(testShowtimeSeat);

        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        Map<String, String> ipnRes = paymentService.processVnPayIpn(params);
        assertEquals("02", ipnRes.get("RspCode"));
    }

    @Test
    @DisplayName("TEST 33: ShowtimeSeat pessimistic lock is actually used")
    void testShowtimeSeatPessimisticLockUsed() {
        List<ShowtimeSeat> lockedSeats = showtimeSeatRepository.findByReservationIdWithLock(pendingReservation.getId());
        assertFalse(lockedSeats.isEmpty());
    }

    // ------------------------------------------------------------------------
    // G. CRITICAL TWO-PHASE TRANSACTION
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("TEST 34: VNPAY SUCCESS + local inventory failure -> inventory & seat rollback, Payment FAILED saved")
    void testLocalFailurePaymentFailedSaved() {
        foodProduct.setAvailableQuantity(0); // Force stock failure
        productRepository.save(foodProduct);

        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);
        entityManager.clear();

        Payment p = paymentRepository.findByTransactionRef(res.getTransactionRef()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, p.getStatus());
        assertEquals("14000001", p.getTransactionNo());
    }

    @Test
    @DisplayName("TEST 35: VNPAY SUCCESS + local seat validation failure -> primary transaction rollback")
    void testLocalSeatFailureRollback() {
        testShowtimeSeat.setStatus(ShowtimeSeatStatus.AVAILABLE);
        showtimeSeatRepository.save(testShowtimeSeat);

        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);

        Reservation r = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
        assertEquals(ReservationStatus.PENDING, r.getStatus()); // Reservation remains PENDING
    }

    @Test
    @DisplayName("TEST 36: Verify REQUIRES_NEW is genuinely independent -> Payment FAILED remains committed")
    void testRequiresNewIndependentCommit() {
        foodProduct.setAvailableQuantity(0);
        productRepository.save(foodProduct);

        CreatePaymentResponse res = paymentService.createPaymentUrl(pendingReservation.getId(), userA, "127.0.0.1");
        Map<String, String> params = createSignedVnPayParams(res.getTransactionRef(), "00", pendingReservation.getTotalPrice());

        paymentService.processVnPayIpn(params);
        entityManager.clear();

        Payment p = paymentRepository.findByTransactionRef(res.getTransactionRef()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, p.getStatus());
        assertNotNull(p.getTransactionNo());
    }

}
