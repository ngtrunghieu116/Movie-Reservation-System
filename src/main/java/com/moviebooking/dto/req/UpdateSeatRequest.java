package com.moviebooking.dto.req;

import com.moviebooking.model.enums.SeatType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSeatRequest {
    private SeatType seatType;
    private Boolean isActive;
}
