package com.moviebooking.dto.res;

import com.moviebooking.model.ShowtimeSeat;
import com.moviebooking.model.enums.SeatType;
import com.moviebooking.model.enums.ShowtimeSeatStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PublicShowtimeSeatResponse {

    private Long seatId;
    private String rowName;
    private Integer seatNumber;
    private SeatType seatType;
    private BigDecimal price;
    private ShowtimeSeatStatus status;

    public static PublicShowtimeSeatResponse fromEntity(ShowtimeSeat showtimeSeat) {
        LocalDateTime now = LocalDateTime.now();
        ShowtimeSeatStatus effectiveStatus = showtimeSeat.getStatus();

        // Dynamic expiration mapping: if HELD and expired -> display AVAILABLE
        if (effectiveStatus == ShowtimeSeatStatus.HELD &&
                showtimeSeat.getLockedUntil() != null &&
                !showtimeSeat.getLockedUntil().isAfter(now)) {
            effectiveStatus = ShowtimeSeatStatus.AVAILABLE;
        }

        return PublicShowtimeSeatResponse.builder()
                .seatId(showtimeSeat.getSeat().getId())
                .rowName(showtimeSeat.getSeat().getRowName())
                .seatNumber(showtimeSeat.getSeat().getSeatNumber())
                .seatType(showtimeSeat.getSeat().getSeatType())
                .price(showtimeSeat.getPrice())
                .status(effectiveStatus)
                .build();
    }
}
