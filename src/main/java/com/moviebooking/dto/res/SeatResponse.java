package com.moviebooking.dto.res;

import com.moviebooking.model.enums.SeatType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SeatResponse {
    private Long id;
    private String rowName;
    private Integer seatNumber;
    private SeatType seatType;
    private Boolean isActive;
    private Long roomId;
}
