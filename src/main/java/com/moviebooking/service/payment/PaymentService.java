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
    private final BookingService bookingService;
    private final PaymentFailureHandler paymentFailureHandler;
    private final com.moviebooking.service.booking.TicketService ticketService;

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
            payment.setStatus(PaymentStatus.FAILED);
            payment.setTransactionNo(transactionNo);
            payment.setBankCode(bankCode);
            paymentRepository.saveAndFlush(payment);
            paymentFailureHandler.recordPaymentFailure(payment.getId(), transactionNo, bankCode, "VNPAY ResponseCode=" + responseCode);
            log.info("[VNPAY_IPN] Payment failed for txnRef {} with ResponseCode {}", txnRef, responseCode);
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        }

        // 5. Case VNPAY ResponseCode == "00" -> Execute Primary Booking Confirmation
        try {
            confirmPrimaryBookingTransaction(payment.getId(), txnRef, transactionNo, bankCode);
            log.info("[VNPAY_IPN] Successfully confirmed booking for txnRef {}", txnRef);
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        } catch (Exception ex) {
            log.error("[VNPAY_PAID_BUT_LOCAL_PROCESSING_FAILED] VNPAY paid for txnRef {}, but local confirmation failed: {}", txnRef, ex.getMessage(), ex);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setTransactionNo(transactionNo);
            payment.setBankCode(bankCode);
            paymentRepository.saveAndFlush(payment);
            // Record Payment FAILED in an independent REQUIRES_NEW transaction
            try {
                paymentFailureHandler.recordPaymentFailure(payment.getId(), transactionNo, bankCode, "VNPAY_PAID_BUT_LOCAL_PROCESSING_FAILED: " + ex.getMessage());
            } catch (Exception e) {
                log.warn("REQUIRES_NEW failure handler fallback: {}", e.getMessage());
            }
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
            throw new InvalidSeatHoldException("Không tìm thấy ghế giữ chỗ cho đơn hàng ID: " + reservation.getId());
        }

        for (ShowtimeSeat ss : showtimeSeats) {
            if (ss.getStatus() != ShowtimeSeatStatus.HELD) {
                throw new InvalidSeatHoldException("Ghế " + ss.getSeat().getRowName() + ss.getSeat().getSeatNumber() + " không còn ở trạng thái giữ chỗ.");
            }
            // Transition HELD -> SOLD
            ss.setStatus(ShowtimeSeatStatus.SOLD);
            ss.setLockedUntil(null);
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

    public ReservationReviewResponse processVnPayReturn(Map<String, String> queryParams, User currentUser) {
        boolean validChecksum = VnPayUtil.verifySignature(queryParams, vnPayConfig.getHashSecret());
        if (!validChecksum) {
            throw new InvalidSeatHoldException("Chữ ký VNPAY không hợp lệ.");
        }

        String txnRef = queryParams.get("vnp_TxnRef");
        Payment payment = paymentRepository.findByTransactionRef(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán: " + txnRef));

        return bookingService.reviewReservation(payment.getReservation().getId(), currentUser);
    }
}
