package com.moviebooking.service.theater;

import com.moviebooking.dto.req.TheaterRequest;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.dto.res.TheaterResponse;

import java.util.List;

public interface ITheaterService {
    List<TheaterResponse> getAllActiveTheaters();
    List<TheaterResponse> getAllTheaters();
    PageResponse<TheaterResponse> getTheatersPaged(int pageNo, int pageSize, String search);
    TheaterResponse getTheaterById(Long id);
    TheaterResponse createTheater(TheaterRequest request);
    TheaterResponse updateTheater(Long id, TheaterRequest request);
    void deleteTheater(Long id);
}
