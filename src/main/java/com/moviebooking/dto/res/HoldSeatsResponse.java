package com.moviebooking.dto.res;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class HoldSeatsResponse {

    private Long showtimeId;
    private String holdToken;
    private List<PublicShowtimeSeatResponse> heldSeats;
    private LocalDateTime expiresAt;
}
