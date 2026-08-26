package com.moviebooking.model;

import com.moviebooking.model.enums.ShowtimeSeatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "showtime_seats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_showtime_seat", columnNames = {"showtime_id", "seat_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ShowtimeSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShowtimeSeatStatus status = ShowtimeSeatStatus.AVAILABLE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Price Snapshot vé cho ghế này tại suất chiếu

    @Column(name = "hold_token", length = 60)
    private String holdToken; // UUID đại diện cho đợt giữ ghế

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by_user_id")
    private User heldByUser; // User đang giữ ghế (Nullable khi status != HELD)

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil; // Hạn thời gian khóa ghế (e.g. now + 8 min)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation; // Đơn hàng đang HELD/SOLD ghế này (Nullable)

    @Version
    private Long version; // Optimistic Locking chống race condition
}

