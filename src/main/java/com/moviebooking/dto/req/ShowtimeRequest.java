package com.moviebooking.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShowtimeRequest {
    @NotNull(message = "ID phim không được để trống")
    private Long movieId;

    @NotNull(message = "ID phòng chiếu không được để trống")
    private Long roomId;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Giá vé tiêu chuẩn không được để trống")
    private BigDecimal priceStandard;

    @NotNull(message = "Giá vé VIP không được để trống")
    private BigDecimal priceVip;

    @NotNull(message = "Giá vé đôi không được để trống")
    private BigDecimal priceCouple;
}
