package com.moviebooking.dto.res;

import com.moviebooking.model.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private Long id;
    private String name;
    private RoomType roomType;
    private Long theaterId;
    private String theaterName;
    private Boolean isActive;
}
