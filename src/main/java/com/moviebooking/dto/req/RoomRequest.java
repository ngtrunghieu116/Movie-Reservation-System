package com.moviebooking.dto.req;

import com.moviebooking.model.enums.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomRequest {

    @NotBlank(message = "Tên phòng chiếu không được để trống")
    @Size(max = 50, message = "Tên phòng chiếu không được vượt quá 50 ký tự")
    private String name;

    @NotNull(message = "Loại phòng chiếu không được để trống")
    private RoomType roomType;

    @NotNull(message = "ID Rạp chiếu không được để trống")
    private Long theaterId;

    private Boolean isActive = true;
}
