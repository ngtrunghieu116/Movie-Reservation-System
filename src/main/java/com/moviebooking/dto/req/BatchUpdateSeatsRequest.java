package com.moviebooking.dto.req;

import com.moviebooking.model.enums.SeatType;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BatchUpdateSeatsRequest {
    @NotEmpty(message = "Danh sách ID ghế không được để trống!")
    private List<Long> seatIds;

    private SeatType seatType;
    private Boolean isActive;
}
