package com.moviebooking.dto.req;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReservationRequest {

    @NotNull(message = "showtimeId không được để trống")
    private Long showtimeId;

    @NotEmpty(message = "Danh sách seatIds không được để trống")
    private List<Long> seatIds;

    @NotNull(message = "holdToken không được để trống")
    private String holdToken;
}
