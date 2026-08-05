package com.moviebooking.service.showtime;

import com.moviebooking.dto.req.ShowtimeRequest;
import com.moviebooking.dto.res.AdminShowtimeResponse;
import com.moviebooking.dto.res.PublicShowtimeResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

public interface IShowtimeService {
    AdminShowtimeResponse createShowtime(ShowtimeRequest request);
    
    AdminShowtimeResponse updateShowtime(Long id, ShowtimeRequest request);
    
    void deleteShowtime(Long id);
    
    AdminShowtimeResponse getShowtimeById(Long id);
    
    Page<AdminShowtimeResponse> getShowtimesByRoom(Long roomId, int page, int size);
    
    Page<AdminShowtimeResponse> searchShowtimes(Long theaterId, Long roomId, Long movieId, LocalDateTime fromDate, LocalDateTime toDate, int page, int size);

    Page<PublicShowtimeResponse> searchPublicShowtimes(Long theaterId, Long roomId, Long movieId, LocalDateTime fromDate, LocalDateTime toDate, int page, int size);
}
