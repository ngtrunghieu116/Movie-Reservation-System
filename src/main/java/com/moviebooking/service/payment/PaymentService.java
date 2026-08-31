package com.moviebooking.service.payment;

import com.moviebooking.config.VnPayConfig;
import com.moviebooking.dto.req.AddComboRequest;
import com.moviebooking.dto.res.CreatePaymentResponse;
import com.moviebooking.dto.res.ReservationReviewResponse;
import com.moviebooking.exception.*;
import com.moviebooking.model.*;
import com.moviebooking.model.enums.PaymentMethod;
import com.moviebooking.model.enums.PaymentStatus;
import com.moviebooking.model.enums.ReservationStatus;
import com.moviebooking.model.enums.ShowtimeSeatStatus;
import com.moviebooking.repository.*;
import com.moviebooking.service.booking.BookingService;
import com.moviebooking.util.VnPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final VnPayConfig vnPayConfig;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final TicketRepository ticketRepository;
    private final BookingService bookingService;
    private final PaymentFailureHandler paymentFailureHandler;
    private final com.moviebooking.service.booking.TicketService ticketService;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private PaymentService self;

    @Transactional
    public CreatePaymentResponse createPaymentUrl(Long reservationId, User currentUser, String clientIp) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + reservationId));

        // 1. Ownership check -> 403
        if (currentUser == null || reservation.getUser() == null || !currentUser.getId().equals(reservation.getUser().getId())) {
            throw new SeatHoldOwnershipException("Bạn không có quyền thao tác trên đơn hàng của người dùng khác.");
        }

        // 2. Status & Expiration checks
        if (reservation.getStatus() == ReservationStatus.CONFIRMED
                || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ReservationNotModifiableException("Đơn hàng đã ở trạng thái không thể thanh toán: " + reservation.getStatus());
        }

        if (reservation.getExpiresAt() != null && !reservation.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ReservationNotModifiableException("Đơn hàng đã hết hạn thanh toán.");
        }

        // 3. Payment status check
        Optional<Payment> existingPaymentOpt = paymentRepository.findByReservationId(reservationId);
        Payment payment;

        if (existingPaymentOpt.isPresent()) {
            payment = existingPaymentOpt.get();
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                throw new ReservationNotModifiableException("Đơn hàng đã được thanh toán thành công.");
            }
        } else {
            payment = new Payment();
            payment.setReservation(reservation);
            payment.setPaymentMethod(PaymentMethod.VNPAY);
        }

        // 4. Generate new unique transactionRef for this payment attempt
        String newTxnRef = "TXN-" + reservationId + "-" + System.currentTimeMillis();
        payment.setTransactionRef(newTxnRef);
        payment.setAmount(reservation.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment = paymentRepository.saveAndFlush(payment);


        // 5. Build VNPAY Sandbox Query Parameters
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", VnPayConfig.VNP_VERSION);
        vnpParams.put("vnp_Command", VnPayConfig.VNP_COMMAND);
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());

        // Amount in VNPAY is multiplied by 100
        long vnpAmount = reservation.getTotalPrice().multiply(BigDecimal.valueOf(100)).longValue();
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", VnPayConfig.VNP_CURRENCY);
        vnpParams.put("vnp_TxnRef", newTxnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + reservation.getBookingCode());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", clientIp != null ? clientIp : "127.0.0.1");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", LocalDateTime.now().format(formatter));
        vnpParams.put("vnp_ExpireDate", LocalDateTime.now().plusMinutes(15).format(formatter));

        String paymentUrl = VnPayUtil.buildQueryString(vnpParams, vnPayConfig.getHashSecret(), vnPayConfig.getVnpayUrl());

        return CreatePaymentResponse.builder()
                .reservationId(reservation.getId())
                .paymentId(payment.getId())
                .transactionRef(newTxnRef)
                .amount(reservation.getTotalPrice())
                .paymentUrl(paymentUrl)
                .status(PaymentStatus.PENDING)
                .build();
    }

    public Map<String, String> processVnPayIpn(Map<String, String> queryParams) {
        // 1. Checksum verification FIRST
        if (!VnPayUtil.verifySignature(queryParams, vnPayConfig.getHashSecret())) {
            log.warn("[VNPAY_IPN] Invalid checksum signature");
            return Map.of("RspCode", "97", "Message", "Fail checksum");
        }

        String txnRef = queryParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        Optional<Payment> paymentOpt = paymentRepository.findByTransactionRef(txnRef);
        if (paymentOpt.isEmpty()) {
            log.warn("[VNPAY_IPN] Payment transactionRef not found: {}", txnRef);
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        Payment payment = paymentOpt.get();
        Reservation reservation = payment.getReservation();

        // 2. Amount verification
        String vnpAmountStr = queryParams.get("vnp_Amount");
        if (vnpAmountStr != null) {
            BigDecimal vnpAmountDecimal = new BigDecimal(vnpAmountStr).divide(BigDecimal.valueOf(100));
            if (vnpAmountDecimal.compareTo(reservation.getTotalPrice()) != 0) {
                log.warn("[VNPAY_IPN] Amount mismatch for txnRef {}: expected {}, got {}", txnRef, reservation.getTotalPrice(), vnpAmountDecimal);
                return Map.of("RspCode", "04", "Message", "Invalid amount");
            }
        }

        // 3. Idempotency Check: Already COMPLETED?
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.info("[VNPAY_IPN] Transaction {} already COMPLETED. Idempotent success returned.", txnRef);
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        }

        String responseCode = queryParams.get("vnp_ResponseCode");
        String transactionNo = queryParams.get("vnp_TransactionNo");
        String bankCode = queryParams.get("vnp_BankCode");

        // 4. Case VNPAY ResponseCode != "00" (Payment Failed at gateway)
        if (!"00".equals(responseCode)) {
            self.executePaymentCancelOrFailure(payment.getId(), transactionNo, bankCode, responseCode, "VNPAY ResponseCode=" + responseCode);
            log.info("[VNPAY_IPN] Payment failed for txnRef {} with ResponseCode {}", txnRef, responseCode);
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        }

        // 5. Case VNPAY ResponseCode == "00" -> Execute Primary Booking Confirmation
        try {
            self.confirmPrimaryBookingTransaction(payment.getId(), txnRef, transactionNo, bankCode);
            log.info("[VNPAY_IPN] Successfully confirmed booking for txnRef {}", txnRef);
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        } catch (Exception ex) {
            log.error("[VNPAY_PAID_BUT_LOCAL_PROCESSING_FAILED] VNPAY paid for txnRef {}, but local confirmation failed: {}", txnRef, ex.getMessage(), ex);
            self.executePaymentCancelOrFailure(payment.getId(), transactionNo, bankCode, responseCode, "Local processing failed: " + ex.getMessage());
            return Map.of("RspCode", "02", "Message", "Order confirm failed");
        }


    }

    @Transactional
    public void confirmPrimaryBookingTransaction(Long paymentId, String txnRef, String transactionNo, String bankCode) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment ID not found: " + paymentId));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return;
        }

        Reservation reservation = payment.getReservation();
        if (reservation.getStatus() == ReservationStatus.EXPIRED || reservation.getStatus() == ReservationStatus.CANCELLED) {
            log.warn("[PAYMENT_REJECTED] VNPAY success callback arrived for EXPIRED/CANCELLED reservationId={}, status={}", reservation.getId(), reservation.getStatus());
            throw new ReservationNotModifiableException("Đơn hàng đã bị hết hạn hoặc hủy, không thể xác nhận thanh toán.");
        }

        // Lock order: 1. Payment -> 2. Products (sorted ascending) -> 3. ShowtimeSeats
        List<OrderItem> orderItems = orderItemRepository.findByReservationId(reservation.getId());
        List<Long> productIds = orderItems.stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Lock Products with PESSIMISTIC_WRITE & Authoritative Inventory Check
        for (Long productId : productIds) {
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm ID: " + productId));

            int totalRequestedQuantity = orderItems.stream()
                    .filter(item -> item.getProduct().getId().equals(productId))
                    .mapToInt(OrderItem::getQuantity)
                    .sum();

            if (product.getAvailableQuantity() == null || product.getAvailableQuantity() < totalRequestedQuantity) {
                throw new InsufficientInventoryException("Sản phẩm '" + product.getName() + "' không đủ tồn kho (Còn: " + product.getAvailableQuantity() + ").");
            }

            // Deduct inventory
            product.setAvailableQuantity(product.getAvailableQuantity() - totalRequestedQuantity);
            productRepository.save(product);
        }

        // Lock ShowtimeSeats with PESSIMISTIC_WRITE
        List<ShowtimeSeat> showtimeSeats = showtimeSeatRepository.findByReservationIdWithLock(reservation.getId());
        if (showtimeSeats.isEmpty()) {
            List<ReservedSeat> rsList = reservedSeatRepository.findByReservationId(reservation.getId());
            List<Long> seatIds = rsList.stream().map(rs -> rs.getSeat().getId()).collect(Collectors.toList());
            if (!seatIds.isEmpty()) {
                showtimeSeats = showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(reservation.getShowtime().getId(), seatIds);
            }
        }
        if (showtimeSeats.isEmpty()) {
            throw new InvalidSeatHoldException("Không tìm thấy ghế giữ chỗ cho đơn hàng ID: " + reservation.getId());
        }

        for (ShowtimeSeat ss : showtimeSeats) {
            if (ss.getStatus() != ShowtimeSeatStatus.HELD) {
                throw new InvalidSeatHoldException("Ghế " + ss.getSeat().getRowName() + ss.getSeat().getSeatNumber() + " không còn ở trạng thái giữ chỗ.");
            }
            // Transition HELD -> SOLD
            ss.setStatus(ShowtimeSeatStatus.SOLD);
            ss.setLockedUntil(null);
            ss.setReservation(reservation);
        }
        showtimeSeatRepository.saveAll(showtimeSeats);

        // Update Reservation & Payment
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        // Phase 5: Generate tickets BEFORE Payment is marked COMPLETED
        ticketService.generateTicketsForReservation(reservation);

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionNo(transactionNo);
        payment.setBankCode(bankCode);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    @Transactional
    public void executePaymentCancelOrFailure(Long paymentId, String transactionNo, String bankCode, String responseCode, String reason) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) return;
        if (payment.getStatus() == PaymentStatus.COMPLETED) return;

        payment.setStatus(PaymentStatus.FAILED);
        if (transactionNo != null) payment.setTransactionNo(transactionNo);
        if (bankCode != null) payment.setBankCode(bankCode);
        paymentRepository.save(payment);

        Reservation reservation = payment.getReservation();
        if (reservation.getStatus() != ReservationStatus.CANCELLED && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);

            List<ShowtimeSeat> seats = showtimeSeatRepository.findByReservationId(reservation.getId());
            for (ShowtimeSeat ss : seats) {
                if (ss.getStatus() == ShowtimeSeatStatus.HELD) {
                    ss.setStatus(ShowtimeSeatStatus.AVAILABLE);
                    ss.setHoldToken(null);
                    ss.setHeldByUser(null);
                    ss.setLockedUntil(null);
                    ss.setReservation(null);
                }
            }
            showtimeSeatRepository.saveAll(seats);
            log.info("[SEATS_RELEASED_INSTANTLY] Đã mở lại {} ghế cho đơn hủy ID: {}", seats.size(), reservation.getId());
        }
        
        try {
            paymentFailureHandler.recordPaymentFailure(paymentId, transactionNo, bankCode, reason);
        } catch (Exception e) {
            log.warn("Failure handler fallback: {}", e.getMessage());
        }
    }

    public ReservationReviewResponse processVnPayReturn(Map<String, String> queryParams, User currentUser) {
        boolean validChecksum = VnPayUtil.verifySignature(queryParams, vnPayConfig.getHashSecret());
        if (!validChecksum) {
            throw new InvalidSeatHoldException("Chữ ký VNPAY không hợp lệ.");
        }

        String txnRef = queryParams.get("vnp_TxnRef");
        Payment payment = paymentRepository.findByTransactionRef(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán: " + txnRef));

        String responseCode = queryParams.get("vnp_ResponseCode");
        String transactionNo = queryParams.get("vnp_TransactionNo");
        String bankCode = queryParams.get("vnp_BankCode");

        // Orchestrate using self to ensure transaction proxy is applied
        if ("00".equals(responseCode)) {
            try {
                self.confirmPrimaryBookingTransaction(payment.getId(), txnRef, transactionNo, bankCode);
                log.info("[VNPAY_RETURN] Successfully confirmed booking for txnRef {}", txnRef);
            } catch (Exception ex) {
                log.error("[VNPAY_RETURN] Confirmation failed for txnRef {}: {}", txnRef, ex.getMessage(), ex);
                self.executePaymentCancelOrFailure(payment.getId(), transactionNo, bankCode, responseCode, "Confirmation Failed: " + ex.getMessage());
            }
        } else {
            self.executePaymentCancelOrFailure(payment.getId(), transactionNo, bankCode, responseCode, "VNPAY Return ResponseCode=" + responseCode);
            log.info("[VNPAY_RETURN] Payment failed/cancelled for txnRef {} with ResponseCode {}", txnRef, responseCode);
        }

        return bookingService.reviewReservation(payment.getReservation().getId(), currentUser);
    }

    @Transactional(readOnly = true)
    public com.moviebooking.dto.res.PaymentStatusDetailResponse getPaymentStatusDetail(String orderId, User currentUser) {
        if (currentUser == null) {
            throw new SeatHoldOwnershipException("Bạn chưa đăng nhập.");
        }

        if (orderId == null || orderId.trim().isEmpty()) {
            throw new ResourceNotFoundException("Mã đơn hàng không hợp lệ.");
        }

        String trimmedOrderId = orderId.trim();
        Reservation reservation = null;

        // 1. Try finding by bookingCode first
        Optional<Reservation> resOpt = reservationRepository.findByBookingCode(trimmedOrderId);
        if (resOpt.isPresent()) {
            reservation = resOpt.get();
        } else {
            // 2. Try parsing numeric ID (for backward compatibility)
            try {
                Long id = Long.parseLong(trimmedOrderId);
                reservation = reservationRepository.findById(id).orElse(null);
            } catch (NumberFormatException ignored) {}
        }

        if (reservation == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn đặt vé với mã: " + trimmedOrderId);
        }

        // 3. Security check: User must own this reservation
        if (reservation.getUser() == null || !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new SeatHoldOwnershipException("Bạn không có quyền truy cập thông tin đơn đặt vé này.");
        }

        // 4. Fetch related domain models
        Showtime showtime = reservation.getShowtime();
        Movie movie = showtime != null ? showtime.getMovie() : null;
        Room room = showtime != null ? showtime.getRoom() : null;
        Theater theater = room != null ? room.getTheater() : null;

        // Payment info
        Optional<Payment> paymentOpt = paymentRepository.findByReservationId(reservation.getId());
        Payment payment = paymentOpt.orElse(null);

        // Reserved seats
        List<ReservedSeat> rsList = reservedSeatRepository.findByReservationId(reservation.getId());
        List<String> seatNames = rsList.stream()
                .map(rs -> rs.getSeat().getRowName() + rs.getSeat().getSeatNumber())
                .collect(Collectors.toList());

        BigDecimal ticketSubtotal = rsList.stream()
                .map(ReservedSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tickets list with QR Code
        List<Ticket> tickets = ticketRepository.findByReservationId(reservation.getId());
        List<com.moviebooking.dto.res.TicketResponse> ticketDTOs = tickets.stream()
                .map(t -> com.moviebooking.dto.res.TicketResponse.builder()
                        .ticketCode(t.getTicketCode())
                        .showtimeId(t.getShowtime().getId())
                        .movieTitle(movie != null ? movie.getTitle() : "")
                        .theaterName(theater != null ? theater.getName() : "")
                        .roomName(room != null ? room.getName() : "")
                        .startTime(showtime != null ? showtime.getStartTime() : null)
                        .seatName(t.getSeat().getRowName() + t.getSeat().getSeatNumber())
                        .price(t.getPrice())
                        .status(t.getStatus())
                        .qrCodeUrl(t.getQrCodeUrl())
                        .checkedInAt(t.getCheckedInAt())
                        .build())
                .collect(Collectors.toList());

        // Order Items (F&B)
        List<OrderItem> orderItems = orderItemRepository.findByReservationId(reservation.getId());
        List<com.moviebooking.dto.res.OrderItemResponse> itemDTOs = orderItems.stream()
                .map(item -> com.moviebooking.dto.res.OrderItemResponse.builder()
                        .itemId(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        BigDecimal fnbSubtotal = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int ticketCount = rsList.isEmpty() ? tickets.size() : rsList.size();

        return com.moviebooking.dto.res.PaymentStatusDetailResponse.builder()
                .reservationId(reservation.getId())
                .bookingCode(reservation.getBookingCode())
                .orderId(reservation.getBookingCode())
                .reservationStatus(reservation.getStatus())
                .transactionDate(payment != null && payment.getPaidAt() != null ? payment.getPaidAt() : reservation.getCreatedAt())
                .customerName(((currentUser.getFirstName() != null ? currentUser.getFirstName() : "") + " " + (currentUser.getLastName() != null ? currentUser.getLastName() : "")).trim())
                .customerEmail(currentUser.getEmail())
                .customerPhone(currentUser.getPhone())
                .movieTitle(movie != null ? movie.getTitle() : "")
                .posterPath(movie != null ? movie.getPosterPath() : null)
                .ageRating(movie != null && movie.getAgeRating() != null ? movie.getAgeRating().name() : null)
                .language(movie != null ? movie.getLanguage() : null)
                .subtitle(movie != null ? movie.getSubtitle() : null)
                .roomType(room != null && room.getRoomType() != null ? room.getRoomType().getDbValue() : "2D")
                .showtimeStart(showtime != null ? showtime.getStartTime() : null)
                .showtimeEnd(showtime != null ? showtime.getEndTime() : null)
                .theaterName(theater != null ? theater.getName() : "")
                .roomName(room != null ? room.getName() : "")
                .ticketCount(ticketCount)
                .seatNames(seatNames)
                .tickets(ticketDTOs)
                .fnbItems(itemDTOs)
                .fnbSubtotal(fnbSubtotal)
                .ticketSubtotal(ticketSubtotal)
                .totalAmount(reservation.getTotalPrice())
                .paymentMethod(payment != null ? payment.getPaymentMethod() : null)
                .transactionNo(payment != null ? payment.getTransactionNo() : null)
                .transactionRef(payment != null ? payment.getTransactionRef() : null)
                .bankCode(payment != null ? payment.getBankCode() : null)
                .amount(payment != null ? payment.getAmount() : reservation.getTotalPrice())
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .paidAt(payment != null ? payment.getPaidAt() : null)
                .build();
    }
}
