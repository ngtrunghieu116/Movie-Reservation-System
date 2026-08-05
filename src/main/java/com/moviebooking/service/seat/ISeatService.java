package com.moviebooking.service.seat;

import com.moviebooking.dto.req.BatchGenerateSeatsRequest;
import com.moviebooking.dto.req.BatchUpdateSeatsRequest;
import com.moviebooking.dto.req.UpdateSeatRequest;
import com.moviebooking.dto.res.SeatResponse;

import java.util.List;

public interface ISeatService {
    List<SeatResponse> getSeatsByRoomId(Long roomId);

    List<SeatResponse> generateSeatLayout(Long roomId, BatchGenerateSeatsRequest request);

    SeatResponse updateSeat(Long seatId, UpdateSeatRequest request);

    List<SeatResponse> batchUpdateSeats(BatchUpdateSeatsRequest request);
}
