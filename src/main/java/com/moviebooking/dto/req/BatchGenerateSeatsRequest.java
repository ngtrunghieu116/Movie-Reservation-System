package com.moviebooking.dto.req;

import com.moviebooking.model.enums.SeatType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class BatchGenerateSeatsRequest {

    @NotBlank(message = "Hàng ghế bắt đầu không được để trống!")
    private String startRow = "A";

    @NotBlank(message = "Hàng ghế kết thúc không được để trống!")
    private String endRow = "F";

    @NotNull(message = "Số ghế trên mỗi hàng không được để trống!")
    @Min(value = 1, message = "Số ghế/hàng tối thiểu là 1")
    @Max(value = 30, message = "Số ghế/hàng tối đa là 30")
    private Integer seatsPerRow = 10;

    private SeatType defaultSeatType = SeatType.STANDARD;

    private Map<String, SeatType> rowSeatTypes;

    private Boolean overrideExisting = false;
}
