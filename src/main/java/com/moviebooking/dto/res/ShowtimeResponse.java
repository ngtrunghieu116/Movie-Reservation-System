package com.moviebooking.dto.res;

import com.moviebooking.model.Showtime;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ShowtimeResponse {
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

    public static ShowtimeResponse fromEntity(Showtime showtime) {
        return ShowtimeResponse.builder()
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
                .build();
    }
}
