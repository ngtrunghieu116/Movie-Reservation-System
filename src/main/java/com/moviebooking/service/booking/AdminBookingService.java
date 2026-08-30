package com.moviebooking.service.booking;

import com.moviebooking.dto.res.AdminBookingDetailResponse;
import com.moviebooking.dto.res.AdminBookingListItemResponse;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.model.*;
import com.moviebooking.model.enums.PaymentStatus;
import com.moviebooking.model.enums.ReservationStatus;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final OrderItemRepository orderItemRepository;
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public Page<AdminBookingListItemResponse> getBookings(
            String search,
            ReservationStatus bookingStatus,
            PaymentStatus paymentStatus,
            LocalDate showtimeDate,
            Pageable pageable) {

        // 1. Chuẩn hóa tham số tìm kiếm & lọc thời gian
        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (showtimeDate != null) {
            startDateTime = showtimeDate.atStartOfDay();
            endDateTime = showtimeDate.atTime(LocalTime.MAX);
        }

        // Đảm bảo sort mặc định createdAt DESC nếu pageable không có sort
        Pageable effectivePageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        // 2. Query page reservations với JOIN FETCH các thực thể To-One (User, Showtime, Movie, Room, Theater)
        Page<Reservation> reservationPage = reservationRepository.searchAdminBookings(
                bookingStatus,
                paymentStatus,
                startDateTime,
                endDateTime,
                searchParam,
                effectivePageable
        );

        if (reservationPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), effectivePageable, reservationPage.getTotalElements());
        }

        List<Reservation> reservations = reservationPage.getContent();
        List<Long> reservationIds = reservations.stream().map(Reservation::getId).toList();

        // 3. Batch query Payments & ReservedSeats cho toàn bộ page (chống N+1 query)
        List<Payment> payments = paymentRepository.findByReservationIdIn(reservationIds);
        Map<Long, Payment> paymentMap = payments.stream()
                .collect(Collectors.toMap(p -> p.getReservation().getId(), p -> p, (p1, p2) -> p1));

        List<ReservedSeat> reservedSeats = reservedSeatRepository.findByReservationIdIn(reservationIds);
        Map<Long, List<String>> seatNamesMap = reservedSeats.stream()
                .collect(Collectors.groupingBy(
                        rs -> rs.getReservation().getId(),
                        Collectors.mapping(rs -> rs.getSeat().getRowName() + rs.getSeat().getSeatNumber(), Collectors.toList())
                ));

        // 4. Map DTO
        List<AdminBookingListItemResponse> items = reservations.stream().map(r -> {
            User user = r.getUser();
            Showtime showtime = r.getShowtime();
            Movie movie = showtime.getMovie();
            Room room = showtime.getRoom();

            String customerName = "";
            if (user != null) {
                String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
                String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
                customerName = (firstName + " " + lastName).trim();
                if (customerName.isEmpty()) {
                    customerName = user.getEmail();
                }
            }

            List<String> seats = seatNamesMap.getOrDefault(r.getId(), Collections.emptyList());
            Payment payment = paymentMap.get(r.getId());
            PaymentStatus pStatus = payment != null ? payment.getStatus() : PaymentStatus.PENDING;

            return AdminBookingListItemResponse.builder()
                    .reservationId(r.getId())
                    .bookingCode(r.getBookingCode())
                    .customerName(customerName)
                    .customerEmail(user != null ? user.getEmail() : null)
                    .customerPhone(user != null ? user.getPhone() : null)
                    .movieTitle(movie != null ? movie.getTitle() : "N/A")
                    .showtimeStart(showtime != null ? showtime.getStartTime() : null)
                    .roomName(room != null ? room.getName() : "N/A")
                    .seatNames(seats)
                    .ticketCount(seats.size())
                    .totalAmount(r.getTotalPrice())
                    .bookingStatus(r.getStatus())
                    .paymentStatus(pStatus)
                    .createdAt(r.getCreatedAt())
                    .build();
        }).toList();

        return new PageImpl<>(items, effectivePageable, reservationPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminBookingDetailResponse getBookingDetail(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt vé với ID: " + reservationId));

        User user = reservation.getUser();
        Showtime showtime = reservation.getShowtime();
        Movie movie = showtime != null ? showtime.getMovie() : null;
        Room room = showtime != null ? showtime.getRoom() : null;
        Theater theater = room != null ? room.getTheater() : null;

        Payment payment = paymentRepository.findByReservationId(reservation.getId()).orElse(null);
        List<ReservedSeat> reservedSeats = reservedSeatRepository.findByReservationId(reservation.getId());
        List<OrderItem> orderItems = orderItemRepository.findByReservationId(reservation.getId());
        List<Ticket> tickets = ticketRepository.findByReservationId(reservation.getId());

        // 1. Customer Name
        String customerName = "";
        if (user != null) {
            String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
            String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
            customerName = (firstName + " " + lastName).trim();
            if (customerName.isEmpty()) {
                customerName = user.getEmail();
            }
        }

        // 2. Seats & Ticket subtotal
        List<String> seatNames = reservedSeats.stream()
                .map(rs -> rs.getSeat().getRowName() + rs.getSeat().getSeatNumber())
                .toList();

        BigDecimal ticketSubtotal = reservedSeats.stream()
                .map(ReservedSeat::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. F&B items & F&B subtotal
        List<AdminBookingDetailResponse.FnbItemDetail> fnbDetails = orderItems.stream().map(oi ->
                AdminBookingDetailResponse.FnbItemDetail.builder()
                        .itemId(oi.getId())
                        .productId(oi.getProduct() != null ? oi.getProduct().getId() : null)
                        .productName(oi.getProduct() != null ? oi.getProduct().getName() : "Sản phẩm")
                        .unitPrice(oi.getUnitPrice())
                        .quantity(oi.getQuantity())
                        .subtotal(oi.getSubtotal())
                        .build()
        ).toList();

        BigDecimal fnbSubtotal = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Tickets detail with QR Code & Check-in info
        List<AdminBookingDetailResponse.TicketAdminDetail> ticketDetails = tickets.stream().map(t ->
                AdminBookingDetailResponse.TicketAdminDetail.builder()
                        .ticketCode(t.getTicketCode())
                        .seatName(t.getSeat() != null ? t.getSeat().getRowName() + t.getSeat().getSeatNumber() : "N/A")
                        .price(t.getPrice())
                        .status(t.getStatus())
                        .qrCodeUrl(t.getQrCodeUrl())
                        .checkedInAt(t.getCheckedInAt())
                        .build()
        ).toList();

        // 5. Build full detail response
        return AdminBookingDetailResponse.builder()
                .customerName(customerName)
                .customerEmail(user != null ? user.getEmail() : null)
                .customerPhone(user != null ? user.getPhone() : null)
                .reservationId(reservation.getId())
                .bookingCode(reservation.getBookingCode())
                .bookingStatus(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .expiresAt(reservation.getExpiresAt())
                .movieTitle(movie != null ? movie.getTitle() : "N/A")
                .posterPath(movie != null ? movie.getPosterPath() : null)
                .ageRating(movie != null ? movie.getAgeRating() : null)
                .language(movie != null ? movie.getLanguage() : null)
                .subtitle(movie != null ? movie.getSubtitle() : null)
                .showtimeStart(showtime != null ? showtime.getStartTime() : null)
                .showtimeEnd(showtime != null ? showtime.getEndTime() : null)
                .theaterName(theater != null ? theater.getName() : "N/A")
                .roomName(room != null ? room.getName() : "N/A")
                .roomType(room != null && room.getRoomType() != null ? room.getRoomType().getDbValue() : "2D")
                .seatNames(seatNames)
                .ticketCount(seatNames.size())
                .ticketSubtotal(ticketSubtotal)
                .fnbItems(fnbDetails)
                .fnbSubtotal(fnbSubtotal)
                .paymentMethod(payment != null ? payment.getPaymentMethod() : null)
                .transactionRef(payment != null ? payment.getTransactionRef() : null)
                .transactionNo(payment != null ? payment.getTransactionNo() : null)
                .bankCode(payment != null ? payment.getBankCode() : null)
                .amount(payment != null ? payment.getAmount() : null)
                .paymentStatus(payment != null ? payment.getStatus() : PaymentStatus.PENDING)
                .paidAt(payment != null ? payment.getPaidAt() : null)
                .totalAmount(reservation.getTotalPrice())
                .tickets(ticketDetails)
                .build();
    }
}
