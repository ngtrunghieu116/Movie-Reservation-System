package com.moviebooking.service.booking;

import com.moviebooking.dto.req.AddComboRequest;
import com.moviebooking.dto.req.CreateReservationRequest;
import com.moviebooking.dto.req.UpdateComboQuantityRequest;
import com.moviebooking.dto.res.OrderItemResponse;
import com.moviebooking.dto.res.ReservationReviewResponse;
import com.moviebooking.dto.res.ReservedSeatDTO;
import com.moviebooking.exception.*;
import com.moviebooking.model.*;
import com.moviebooking.model.enums.PaymentStatus;
import com.moviebooking.model.enums.ReservationStatus;
import com.moviebooking.model.enums.ShowtimeSeatStatus;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final ReservationRepository reservationRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;

    @Transactional
    public ReservationReviewResponse createReservation(CreateReservationRequest request, User currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Người dùng chưa đăng nhập hoặc phiên làm việc đã hết hạn");
        }
        if (request.getShowtimeId() == null) {
            throw new ResourceNotFoundException("showtimeId không được để trống");
        }
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new InvalidSeatHoldException("Danh sách seatIds không được để trống");
        }
        if (request.getHoldToken() == null || request.getHoldToken().isBlank()) {
            throw new InvalidSeatHoldException("holdToken không được để trống");
        }

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu với ID: " + request.getShowtimeId()));

        if (Boolean.FALSE.equals(showtime.getIsActive())) {
            throw new ShowtimeNotBookableException("Suất chiếu hiện đang bị vô hiệu hóa");
        }
        if (Boolean.FALSE.equals(showtime.getIsOnlineSelling())) {
            throw new ShowtimeNotBookableException("Suất chiếu tạm dừng bán vé trực tuyến");
        }
        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ShowtimeNotBookableException("Suất chiếu đã bắt đầu hoặc đã qua giờ chiếu");
        }

        List<Long> sortedSeatIds = request.getSeatIds().stream()
                .distinct()
                .sorted()
                .toList();

        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtime.getId(), sortedSeatIds);

        if (seats.size() != sortedSeatIds.size()) {
            throw new InvalidSeatHoldException("Một số ghế yêu cầu không tồn tại trong suất chiếu này");
        }

        LocalDateTime now = LocalDateTime.now();

        for (ShowtimeSeat ss : seats) {
            if (ss.getStatus() == ShowtimeSeatStatus.SOLD) {
                throw new SeatAlreadyReservedException("Ghế " + ss.getSeat().getRowName() + ss.getSeat().getSeatNumber() + " đã được bán");
            }
            if (ss.getStatus() != ShowtimeSeatStatus.HELD) {
                throw new InvalidSeatHoldException("Ghế " + ss.getSeat().getRowName() + ss.getSeat().getSeatNumber() + " không ở trạng thái giữ chỗ");
            }
            if (!request.getHoldToken().equals(ss.getHoldToken())) {
                throw new InvalidSeatHoldException("Mã holdToken không khớp cho ghế " + ss.getSeat().getRowName() + ss.getSeat().getSeatNumber());
            }
            if (ss.getHeldByUser() == null || !ss.getHeldByUser().getId().equals(currentUser.getId())) {
                throw new SeatHoldOwnershipException("Bạn không có quyền thao tác trên ghế giữ chỗ của người dùng khác");
            }
            if (ss.getLockedUntil() == null || !ss.getLockedUntil().isAfter(now)) {
                throw new InvalidSeatHoldException("Thời gian giữ ghế " + ss.getSeat().getRowName() + ss.getSeat().getSeatNumber() + " đã hết hạn");
            }
        }

        // Idempotency check:
        Long firstReservationId = seats.get(0).getReservation() != null ? seats.get(0).getReservation().getId() : null;
        if (firstReservationId != null) {
            boolean allSameReservation = seats.stream()
                    .allMatch(ss -> ss.getReservation() != null && ss.getReservation().getId().equals(firstReservationId));
            if (allSameReservation) {
                Reservation existingReservation = reservationRepository.findById(firstReservationId)
                        .orElse(null);
                if (existingReservation != null
                        && existingReservation.getUser() != null
                        && existingReservation.getUser().getId().equals(currentUser.getId())
                        && existingReservation.getStatus() == ReservationStatus.PENDING
                        && existingReservation.getExpiresAt() != null
                        && existingReservation.getExpiresAt().isAfter(now)) {
                    return reviewReservation(existingReservation.getId(), currentUser);
                }
            }
            throw new SeatAlreadyReservedException("Một số ghế đã được gắn vào một đơn hàng khác");
        }

        boolean anyHasReservation = seats.stream().anyMatch(ss -> ss.getReservation() != null);
        if (anyHasReservation) {
            throw new SeatAlreadyReservedException("Một số ghế đã được gắn vào một đơn hàng khác");
        }

        BigDecimal ticketTotal = seats.stream()
                .map(ShowtimeSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime expiresAt = seats.get(0).getLockedUntil();
        String bookingCode = "REV-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Reservation reservation = Reservation.builder()
                .bookingCode(bookingCode)
                .user(currentUser)
                .showtime(showtime)
                .totalPrice(ticketTotal)
                .status(ReservationStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        List<ReservedSeat> reservedSeats = new ArrayList<>();
        for (ShowtimeSeat ss : seats) {
            ReservedSeat rs = ReservedSeat.builder()
                    .reservation(savedReservation)
                    .seat(ss.getSeat())
                    .price(ss.getPrice())
                    .build();
            reservedSeats.add(rs);

            // Link ShowtimeSeat to Reservation without changing status (remains HELD)
            ss.setReservation(savedReservation);
        }

        reservedSeatRepository.saveAll(reservedSeats);
        showtimeSeatRepository.saveAll(seats);

        return reviewReservation(savedReservation.getId(), currentUser);
    }

    public void validateReservationModifiable(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.CONFIRMED
                || reservation.getStatus() == ReservationStatus.EXPIRED
                || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ReservationNotModifiableException("Không thể sửa đổi F&B của đơn hàng ở trạng thái: " + reservation.getStatus());
        }

        if (reservation.getExpiresAt() != null && !reservation.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ReservationNotModifiableException("Đơn hàng đã hết hạn thanh toán.");
        }

        Optional<Payment> paymentOpt = paymentRepository.findByReservationId(reservation.getId());
        if (paymentOpt.isPresent() && paymentOpt.get().getStatus() == PaymentStatus.COMPLETED) {
            throw new ReservationNotModifiableException("Không thể sửa đổi F&B của đơn hàng đã thanh toán thành công.");
        }
    }

    private void validateOwnership(Reservation reservation, User currentUser) {
        if (currentUser == null || reservation.getUser() == null || !currentUser.getId().equals(reservation.getUser().getId())) {
            throw new SeatHoldOwnershipException("Bạn không có quyền thao tác trên đơn hàng của người dùng khác.");
        }
    }

    @Transactional
    public ReservationReviewResponse addComboToReservation(Long reservationId, AddComboRequest request, User currentUser) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + reservationId));

        validateOwnership(reservation, currentUser);
        validateReservationModifiable(reservation);

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new InvalidQuantityException("Số lượng sản phẩm phải lớn hơn 0.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + request.getProductId()));

        if (product.getIsActive() == null || !product.getIsActive()) {
            throw new ProductInactiveException("Sản phẩm '" + product.getName() + "' hiện không còn kinh doanh.");
        }

        List<OrderItem> existingItems = orderItemRepository.findByReservationId(reservationId);
        Optional<OrderItem> duplicateItemOpt = existingItems.stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (duplicateItemOpt.isPresent()) {
            // Duplicate product merge behavior
            OrderItem existingItem = duplicateItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            if (product.getAvailableQuantity() != null && newQuantity > product.getAvailableQuantity()) {
                throw new InsufficientInventoryException("Số lượng tồn kho sản phẩm '" + product.getName() + "' không đủ (Còn: " + product.getAvailableQuantity() + ").");
            }

            existingItem.setQuantity(newQuantity);
            // Preserve existing unitPrice price snapshot
            existingItem.setSubtotal(existingItem.getUnitPrice().multiply(BigDecimal.valueOf(newQuantity)));
            orderItemRepository.save(existingItem);
        } else {
            // New item soft check
            if (product.getAvailableQuantity() != null && request.getQuantity() > product.getAvailableQuantity()) {
                throw new InsufficientInventoryException("Số lượng tồn kho sản phẩm '" + product.getName() + "' không đủ (Còn: " + product.getAvailableQuantity() + ").");
            }

            // Price snapshot at addition moment
            BigDecimal unitPriceSnapshot = product.getPrice();
            BigDecimal subtotal = unitPriceSnapshot.multiply(BigDecimal.valueOf(request.getQuantity()));

            OrderItem newItem = OrderItem.builder()
                    .reservation(reservation)
                    .product(product)
                    .unitPrice(unitPriceSnapshot)
                    .quantity(request.getQuantity())
                    .subtotal(subtotal)
                    .build();
            orderItemRepository.save(newItem);
        }

        return reviewReservation(reservationId, currentUser);
    }

    @Transactional
    public ReservationReviewResponse updateComboQuantity(Long reservationId, Long itemId, UpdateComboQuantityRequest request, User currentUser) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + reservationId));

        validateOwnership(reservation, currentUser);
        validateReservationModifiable(reservation);

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new InvalidQuantityException("Số lượng sản phẩm phải lớn hơn 0.");
        }

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn trong đơn hàng với ID: " + itemId));

        if (!item.getReservation().getId().equals(reservationId)) {
            throw new ResourceNotFoundException("Món ăn không thuộc về đơn hàng này.");
        }

        Product product = item.getProduct();
        if (product.getAvailableQuantity() != null && request.getQuantity() > product.getAvailableQuantity()) {
            throw new InsufficientInventoryException("Số lượng tồn kho sản phẩm '" + product.getName() + "' không đủ (Còn: " + product.getAvailableQuantity() + ").");
        }

        item.setQuantity(request.getQuantity());
        // Preserve unitPrice price snapshot
        item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        orderItemRepository.save(item);

        return reviewReservation(reservationId, currentUser);
    }

    @Transactional
    public ReservationReviewResponse removeComboFromReservation(Long reservationId, Long itemId, User currentUser) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + reservationId));

        validateOwnership(reservation, currentUser);
        validateReservationModifiable(reservation);

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn trong đơn hàng với ID: " + itemId));

        if (!item.getReservation().getId().equals(reservationId)) {
            throw new ResourceNotFoundException("Món ăn không thuộc về đơn hàng này.");
        }

        orderItemRepository.delete(item);

        return reviewReservation(reservationId, currentUser);
    }

    @Transactional
    public ReservationReviewResponse reviewReservation(Long reservationId, User currentUser) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + reservationId));

        validateOwnership(reservation, currentUser);

        List<ReservedSeat> reservedSeats = reservedSeatRepository.findByReservationId(reservationId);
        List<ReservedSeatDTO> ticketSeatDTOs = reservedSeats.stream()
                .map(rs -> ReservedSeatDTO.builder()
                        .seatId(rs.getSeat().getId())
                        .rowName(rs.getSeat().getRowName())
                        .seatNumber(rs.getSeat().getSeatNumber())
                        .price(rs.getPrice())
                        .build())
                .collect(Collectors.toList());

        BigDecimal ticketSubtotal = reservedSeats.stream()
                .map(ReservedSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OrderItem> orderItems = orderItemRepository.findByReservationId(reservationId);
        List<OrderItemResponse> itemDTOs = orderItems.stream()
                .map(item -> OrderItemResponse.builder()
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

        BigDecimal totalAmount = ticketSubtotal.add(fnbSubtotal);

        reservation.setTotalPrice(totalAmount);
        reservationRepository.save(reservation);

        return ReservationReviewResponse.builder()
                .reservationId(reservation.getId())
                .bookingCode(reservation.getBookingCode())
                .movieTitle(reservation.getShowtime() != null && reservation.getShowtime().getMovie() != null ? reservation.getShowtime().getMovie().getTitle() : "")
                .showtimeStart(reservation.getShowtime() != null ? reservation.getShowtime().getStartTime() : null)
                .ticketSeats(ticketSeatDTOs)
                .ticketSubtotal(ticketSubtotal)
                .items(itemDTOs)
                .fnbSubtotal(fnbSubtotal)
                .totalAmount(totalAmount)
                .status(reservation.getStatus())
                .expiresAt(reservation.getExpiresAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<com.moviebooking.dto.res.ReservationHistoryResponse> getBookingHistory(User currentUser) {
        List<Reservation> reservations = reservationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        List<com.moviebooking.dto.res.ReservationHistoryResponse> history = new ArrayList<>();
        
        for (Reservation res : reservations) {
            List<ReservedSeat> rsList = reservedSeatRepository.findByReservationId(res.getId());
            List<ReservedSeatDTO> seatDTOs = rsList.stream()
                    .map(rs -> ReservedSeatDTO.builder()
                            .seatId(rs.getSeat().getId())
                            .rowName(rs.getSeat().getRowName())
                            .seatNumber(rs.getSeat().getSeatNumber())
                            .price(rs.getPrice())
                            .build())
                    .collect(Collectors.toList());
                    
            BigDecimal ticketSub = rsList.stream().map(ReservedSeat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            
            List<OrderItem> items = orderItemRepository.findByReservationId(res.getId());
            List<OrderItemResponse> itemDTOs = items.stream()
                    .map(item -> OrderItemResponse.builder()
                            .itemId(item.getId())
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .unitPrice(item.getUnitPrice())
                            .quantity(item.getQuantity())
                            .subtotal(item.getSubtotal())
                            .build())
                    .collect(Collectors.toList());
                    
            BigDecimal fnbSub = items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Optional<Payment> payment = paymentRepository.findByReservationId(res.getId());
            PaymentStatus pStatus = payment.map(Payment::getStatus).orElse(null);
            
            List<Ticket> tickets = ticketRepository.findByReservationId(res.getId());
            List<com.moviebooking.dto.res.TicketResponse> ticketDTOs = tickets.stream()
                    .map(ticket -> com.moviebooking.dto.res.TicketResponse.builder()
                            .ticketCode(ticket.getTicketCode())
                            .showtimeId(ticket.getShowtime().getId())
                            .movieTitle(ticket.getShowtime().getMovie().getTitle())
                            .theaterName(ticket.getShowtime().getRoom().getTheater().getName())
                            .roomName(ticket.getShowtime().getRoom().getName())
                            .startTime(ticket.getShowtime().getStartTime())
                            .seatName(ticket.getSeat().getRowName() + ticket.getSeat().getSeatNumber())
                            .price(ticket.getPrice())
                            .status(ticket.getStatus())
                            .qrCodeUrl(ticket.getQrCodeUrl())
                            .checkedInAt(ticket.getCheckedInAt())
                            .build())
                    .collect(Collectors.toList());
            
            history.add(com.moviebooking.dto.res.ReservationHistoryResponse.builder()
                    .reservationId(res.getId())
                    .bookingCode(res.getBookingCode())
                    .movieTitle(res.getShowtime().getMovie().getTitle())
                    .theaterName(res.getShowtime().getRoom().getTheater().getName())
                    .roomName(res.getShowtime().getRoom().getName())
                    .showtimeStart(res.getShowtime().getStartTime())
                    .status(res.getStatus())
                    .paymentStatus(pStatus)
                    .ticketSubtotal(ticketSub)
                    .fnbSubtotal(fnbSub)
                    .totalAmount(res.getTotalPrice())
                    .createdAt(res.getCreatedAt())
                    .ticketSeats(seatDTOs)
                    .fnbItems(itemDTOs)
                    .tickets(ticketDTOs)
                    .build());
        }
        
        return history;
    }

    @Transactional(readOnly = true)
    public List<com.moviebooking.dto.res.UserBookingHistoryItemResponse> getMyBookingHistory(User currentUser) {
        if (currentUser == null) {
            throw new SeatHoldOwnershipException("Bạn chưa đăng nhập.");
        }

        List<Reservation> reservations = reservationRepository.findSuccessfulReservationsByUserId(currentUser.getId());
        List<com.moviebooking.dto.res.UserBookingHistoryItemResponse> history = new ArrayList<>();

        for (Reservation res : reservations) {
            int ticketCount = reservedSeatRepository.countByReservationId(res.getId());
            if (ticketCount == 0) {
                List<Ticket> tickets = ticketRepository.findByReservationId(res.getId());
                ticketCount = tickets.size();
            }

            String movieTitle = res.getShowtime() != null && res.getShowtime().getMovie() != null
                    ? res.getShowtime().getMovie().getTitle()
                    : "";

            history.add(com.moviebooking.dto.res.UserBookingHistoryItemResponse.builder()
                    .reservationId(res.getId())
                    .orderId(res.getBookingCode())
                    .transactionDate(res.getCreatedAt())
                    .movieTitle(movieTitle)
                    .transactionType("Mua online")
                    .ticketCount(ticketCount)
                    .totalAmount(res.getTotalPrice())
                    .build());
        }

        return history;
    }

    @Transactional
    public int cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = reservationRepository.findExpiredPendingReservations(ReservationStatus.PENDING, now);
        if (expiredReservations.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Reservation res : expiredReservations) {
            res.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(res);

            Optional<Payment> paymentOpt = paymentRepository.findByReservationId(res.getId());
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                }
            }

            List<ShowtimeSeat> seats = showtimeSeatRepository.findByReservationId(res.getId());
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
            count++;
            log.info("[RESERVATION_EXPIRED_CLEANUP] Expired reservationId={}, bookingCode={}", res.getId(), res.getBookingCode());
        }
        return count;
    }
}
