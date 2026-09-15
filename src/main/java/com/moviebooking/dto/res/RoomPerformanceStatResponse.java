package com.moviebooking.dto.res;

import java.math.BigDecimal;

public record RoomPerformanceStatResponse(
        Long roomId,
        String roomName,
        String roomType,
        Long showtimesCount,
        Long ticketsSold,
        BigDecimal revenue
) {}
