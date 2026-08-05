package com.moviebooking.dto.res;

import com.moviebooking.model.Showtime;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminShowtimeResponse {
    private Long id;
    
    // Movie summary
    private Long movieId;
    private String movieTitle;
    private Integer movieDuration;
    
    // Room summary
    private Long roomId;
    private String roomName;
    private Long theaterId;
    private String theaterName;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private BigDecimal priceStandard;
    private BigDecimal priceVip;
    private BigDecimal priceCouple;

    // Admin specific data
    private Long bookedSeats;
    private Long availableSeats;
    private String status; // "AVAILABLE", "SOLD_OUT", "PAST"

    public static AdminShowtimeResponse fromEntity(Showtime showtime, long bookedSeats, long availableSeats) {
        String status;
        if (LocalDateTime.now().isAfter(showtime.getStartTime())) {
            status = "PAST";
        } else if (availableSeats <= 0) {
            status = "SOLD_OUT";
        } else {
            status = "AVAILABLE";
        }

        return AdminShowtimeResponse.builder()
                .id(showtime.getId())
                .movieId(showtime.getMovie().getId())
                .movieTitle(showtime.getMovie().getTitle())
                .movieDuration(showtime.getMovie().getDuration())
                .roomId(showtime.getRoom().getId())
                .roomName(showtime.getRoom().getName())
                .theaterId(showtime.getRoom().getTheater().getId())
                .theaterName(showtime.getRoom().getTheater().getName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .priceStandard(showtime.getPriceStandard())
                .priceVip(showtime.getPriceVip())
                .priceCouple(showtime.getPriceCouple())
                .bookedSeats(bookedSeats)
                .availableSeats(availableSeats)
                .status(status)
                .build();
    }
}
