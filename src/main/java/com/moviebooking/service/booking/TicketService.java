package com.moviebooking.service.booking;

import com.moviebooking.dto.req.ValidateTicketRequest;
import com.moviebooking.dto.res.TicketResponse;
import com.moviebooking.exception.*;
import com.moviebooking.model.*;
import com.moviebooking.model.enums.PaymentStatus;
import com.moviebooking.model.enums.ReservationStatus;
import com.moviebooking.model.enums.TicketStatus;
import com.moviebooking.repository.PaymentRepository;
import com.moviebooking.repository.ReservedSeatRepository;
import com.moviebooking.repository.TicketRepository;
import com.moviebooking.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public void generateTicketsForReservation(Reservation reservation) {
        // 1. Verify Payment and Reservation status
        // (Wait, at the time of calling this, payment.setStatus(COMPLETED) hasn't been saved yet, but we will check later or trust the caller. The prompt says: Verify Payment.status == COMPLETED, Reservation.status == CONFIRMED.)
        // But the prompt also says: "ONLY after: Reservation.status = CONFIRMED, ShowtimeSeat.status = SOLD but BEFORE: Payment.status = COMPLETED". So Payment is still PENDING in DB. 
        // We will just verify reservation status.
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("Cannot generate ticket: Reservation is not CONFIRMED");
        }

        List<ReservedSeat> reservedSeats = reservedSeatRepository.findByReservationId(reservation.getId());
        
        for (ReservedSeat rs : reservedSeats) {
            Optional<Ticket> existingTicket = ticketRepository.findByReservationIdAndSeatId(reservation.getId(), rs.getSeat().getId());
            if (existingTicket.isPresent()) {
                continue; // Do not create another ticket
            }

            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String randomStr = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            String ticketCode = "TKT-" + dateStr + "-" + randomStr;
            
            // Loop in case of rare collision
            while (ticketRepository.existsByTicketCode(ticketCode)) {
                randomStr = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
                ticketCode = "TKT-" + dateStr + "-" + randomStr;
            }

            String qrCodeUrl = QrCodeUtil.generateQrCodeBase64(ticketCode);

            Ticket ticket = Ticket.builder()
                    .ticketCode(ticketCode)
                    .price(rs.getPrice())
                    .status(TicketStatus.ISSUED)
                    .checkedInAt(null)
                    .qrCodeUrl(qrCodeUrl)
                    .reservation(reservation)
                    .seat(rs.getSeat())
                    .showtime(reservation.getShowtime())
                    .build();

            ticketRepository.save(ticket);
        }
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketByCode(String ticketCode, User currentUser) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new TicketNotFoundException("Không tìm thấy vé: " + ticketCode));

        if (currentUser == null || !ticket.getReservation().getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedTicketAccessException("Bạn không có quyền truy cập vé này.");
        }

        return mapToResponse(ticket);
    }

    @Transactional(readOnly = true)
    public void validateTicket(ValidateTicketRequest request) {
        Ticket ticket = ticketRepository.findByTicketCode(request.getTicketCode())
                .orElseThrow(() -> new TicketNotFoundException("Không tìm thấy vé: " + request.getTicketCode()));
        
        Reservation reservation = ticket.getReservation();
        if (reservation == null) {
            throw new TicketValidationException("Vé không hợp lệ.");
        }
        
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new TicketValidationException("Đơn hàng chưa được xác nhận.");
        }
        
        Payment payment = paymentRepository.findByReservationId(reservation.getId())
                .orElseThrow(() -> new TicketValidationException("Không tìm thấy thông tin thanh toán."));
                
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new TicketValidationException("Thanh toán chưa hoàn tất.");
        }

        if (ticket.getStatus() == TicketStatus.CHECKED_IN || ticket.getStatus() == TicketStatus.USED) {
            throw new TicketAlreadyCheckedInException("Vé đã được sử dụng/check-in.");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new TicketValidationException("Vé đã bị hủy.");
        }
        
        if (ticket.getStatus() != TicketStatus.ISSUED && ticket.getStatus() != TicketStatus.VALID) {
            throw new TicketValidationException("Trạng thái vé không hợp lệ: " + ticket.getStatus());
        }
    }

    @Transactional
    public void checkInTicket(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCodeWithLock(ticketCode)
                .orElseThrow(() -> new TicketNotFoundException("Không tìm thấy vé: " + ticketCode));
        
        Reservation reservation = ticket.getReservation();
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new TicketValidationException("Đơn hàng chưa được xác nhận.");
        }
        
        Payment payment = paymentRepository.findByReservationId(reservation.getId())
                .orElseThrow(() -> new TicketValidationException("Không tìm thấy thông tin thanh toán."));
                
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new TicketValidationException("Thanh toán chưa hoàn tất.");
        }

        if (ticket.getStatus() == TicketStatus.CHECKED_IN || ticket.getStatus() == TicketStatus.USED) {
            throw new TicketAlreadyCheckedInException("Vé đã được sử dụng/check-in.");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new TicketValidationException("Vé đã bị hủy.");
        }
        
        if (ticket.getStatus() != TicketStatus.ISSUED && ticket.getStatus() != TicketStatus.VALID) {
            throw new TicketValidationException("Trạng thái vé không hợp lệ: " + ticket.getStatus());
        }
        
        ticket.setStatus(TicketStatus.CHECKED_IN);
        ticket.setCheckedInAt(LocalDateTime.now());
        ticketRepository.save(ticket);
    }
    
    private TicketResponse mapToResponse(Ticket ticket) {
        return TicketResponse.builder()
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
                .build();
    }
}
