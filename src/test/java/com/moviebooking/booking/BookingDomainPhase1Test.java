package com.moviebooking.booking;

import com.moviebooking.model.*;
import com.moviebooking.model.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BookingDomainPhase1Test {

    @Test
    @DisplayName("Test ShowtimeSeat status and price snapshot initialization")
    void testShowtimeSeatDomain() {
        Showtime showtime = Showtime.builder().id(1L).build();
        Seat seat = Seat.builder().id(10L).rowName("A").seatNumber(1).seatType(SeatType.VIP).build();

        ShowtimeSeat showtimeSeat = ShowtimeSeat.builder()
                .showtime(showtime)
                .seat(seat)
                .price(new BigDecimal("110000.00"))
                .status(ShowtimeSeatStatus.AVAILABLE)
                .build();

        assertEquals(ShowtimeSeatStatus.AVAILABLE, showtimeSeat.getStatus());
        assertEquals(new BigDecimal("110000.00"), showtimeSeat.getPrice());
        assertNull(showtimeSeat.getLockedUntil());
        assertNull(showtimeSeat.getReservation());

        // Simulate seat hold
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(8);
        showtimeSeat.setStatus(ShowtimeSeatStatus.HELD);
        showtimeSeat.setLockedUntil(expireTime);

        assertEquals(ShowtimeSeatStatus.HELD, showtimeSeat.getStatus());
        assertEquals(expireTime, showtimeSeat.getLockedUntil());
    }

    @Test
    @DisplayName("Test ReservedSeat and OrderItem price snapshots")
    void testPriceSnapshots() {
        Reservation reservation = Reservation.builder().id(100L).bookingCode("BK-100").build();
        Seat seat = Seat.builder().id(5L).build();
        Product product = Product.builder().id(2L).name("Combo Popcorn").build();

        ReservedSeat reservedSeat = ReservedSeat.builder()
                .reservation(reservation)
                .seat(seat)
                .price(new BigDecimal("90000.00")) // Ticket price snapshot
                .build();

        OrderItem orderItem = OrderItem.builder()
                .reservation(reservation)
                .product(product)
                .unitPrice(new BigDecimal("85000.00")) // F&B unit price snapshot
                .quantity(2)
                .subtotal(new BigDecimal("170000.00"))
                .build();

        assertEquals(new BigDecimal("90000.00"), reservedSeat.getPrice());
        assertEquals(new BigDecimal("85000.00"), orderItem.getUnitPrice());
        assertEquals(new BigDecimal("170000.00"), orderItem.getSubtotal());
    }

    @Test
    @DisplayName("Test Payment and Ticket entity fields")
    void testPaymentAndTicketDomain() {
        Reservation reservation = Reservation.builder().id(200L).bookingCode("BK-200").build();

        Payment payment = Payment.builder()
                .reservation(reservation)
                .transactionRef("VNP-TXN-20260826-001")
                .paymentMethod(PaymentMethod.VNPAY)
                .amount(new BigDecimal("260000.00"))
                .status(PaymentStatus.PENDING)
                .build();

        Ticket ticket = Ticket.builder()
                .ticketCode("TK-20260826-A8F91")
                .reservation(reservation)
                .price(new BigDecimal("90000.00"))
                .status(TicketStatus.VALID)
                .build();

        assertEquals("VNP-TXN-20260826-001", payment.getTransactionRef());
        assertEquals(PaymentMethod.VNPAY, payment.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());

        assertEquals("TK-20260826-A8F91", ticket.getTicketCode());
        assertEquals(TicketStatus.VALID, ticket.getStatus());
    }
}
