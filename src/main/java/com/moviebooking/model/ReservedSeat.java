package com.moviebooking.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "reserved_seats")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ReservedSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Price Snapshot vé tại thời điểm đặt
}

