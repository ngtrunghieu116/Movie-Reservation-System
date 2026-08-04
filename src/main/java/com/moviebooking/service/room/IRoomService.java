package com.moviebooking.service.room;

import com.moviebooking.dto.req.RoomRequest;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.dto.res.RoomResponse;

import java.util.List;

public interface IRoomService {
    List<RoomResponse> getActiveRoomsByTheaterId(Long theaterId);
    PageResponse<RoomResponse> getRoomsPaged(int pageNo, int pageSize, Long theaterId, String search);
    RoomResponse getRoomById(Long id);
    RoomResponse createRoom(RoomRequest request);
    RoomResponse updateRoom(Long id, RoomRequest request);
    void deleteRoom(Long id);
}
