package com.moviebooking.dto.res;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservedSeatDTO {
    private Long seatId;
    private String rowName;
    private Integer seatNumber;
    private BigDecimal price;
}
