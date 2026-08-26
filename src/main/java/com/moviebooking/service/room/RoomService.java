package com.moviebooking.service.room;

import com.moviebooking.context.PrimaryCinemaContext;
import com.moviebooking.dto.req.RoomRequest;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.dto.res.RoomResponse;
import com.moviebooking.model.Room;
import com.moviebooking.model.Theater;
import com.moviebooking.repository.RoomRepository;
import com.moviebooking.repository.SeatRepository;
import com.moviebooking.repository.ShowtimeRepository;
import com.moviebooking.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService implements IRoomService {

    private final RoomRepository roomRepository;
    private final TheaterRepository theaterRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final PrimaryCinemaContext primaryCinemaContext;

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getActiveRoomsByTheaterId(Long theaterId) {
        Long targetTheaterId = theaterId != null ? theaterId : primaryCinemaContext.getPrimaryTheaterId();
        return roomRepository.findByTheaterIdAndIsActiveTrue(targetTheaterId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> getRoomsPaged(int pageNo, int pageSize, Long theaterId, String search) {
        Long targetTheaterId = theaterId != null ? theaterId : primaryCinemaContext.getPrimaryTheaterId();
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        Page<Room> roomPage;

        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (hasSearch) {
            roomPage = roomRepository.findByTheaterIdAndNameContainingIgnoreCase(targetTheaterId, search.trim(), pageable);
        } else {
            roomPage = roomRepository.findByTheaterId(targetTheaterId, pageable);
        }

        List<RoomResponse> content = roomPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<RoomResponse>builder()
                .content(content)
                .pageNo(roomPage.getNumber())
                .pageSize(roomPage.getSize())
                .totalElements(roomPage.getTotalElements())
                .totalPages(roomPage.getTotalPages())
                .last(roomPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng chiếu với ID: " + id));
        return mapToResponse(room);
    }

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        Long targetTheaterId = request.getTheaterId() != null ? request.getTheaterId() : primaryCinemaContext.getPrimaryTheaterId();
        Theater theater = theaterRepository.findById(targetTheaterId)
                .orElseThrow(() -> new RuntimeException("Rạp chiếu được chọn không tồn tại!"));

        if (roomRepository.existsByNameAndTheaterId(request.getName(), targetTheaterId)) {
            throw new RuntimeException("Tên phòng chiếu '" + request.getName() + "' đã tồn tại trong cơ sở rạp này!");
        }

        Room room = Room.builder()
                .name(request.getName())
                .roomType(request.getRoomType())
                .theater(theater)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Room savedRoom = roomRepository.save(room);
        return mapToResponse(savedRoom);
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng chiếu với ID: " + id));

        Long targetTheaterId = request.getTheaterId() != null ? request.getTheaterId() : primaryCinemaContext.getPrimaryTheaterId();
        Theater theater = theaterRepository.findById(targetTheaterId)
                .orElseThrow(() -> new RuntimeException("Rạp chiếu được chọn không tồn tại!"));

        if (roomRepository.existsByNameAndTheaterIdAndIdNot(request.getName(), targetTheaterId, id)) {
            throw new RuntimeException("Tên phòng chiếu '" + request.getName() + "' đã trùng với phòng khác trong cùng cơ sở!");
        }

        room.setName(request.getName());
        room.setRoomType(request.getRoomType());
        room.setTheater(theater);
        if (request.getIsActive() != null) {
            room.setIsActive(request.getIsActive());
        }

        Room updatedRoom = roomRepository.save(room);
        return mapToResponse(updatedRoom);
    }


    @Override
    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng chiếu với ID: " + id));

        if (showtimeRepository.existsByRoomIdAndEndTimeAfter(id, LocalDateTime.now())) {
            throw new IllegalStateException("Không thể xóa phòng chiếu vì phòng chiếu đang có lịch chiếu sắp diễn ra!");
        }

        seatRepository.deleteByRoomId(id);
        seatRepository.flush();
        roomRepository.delete(room);
    }

    private RoomResponse mapToResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .roomType(room.getRoomType())
                .theaterId(room.getTheater() != null ? room.getTheater().getId() : null)
                .theaterName(room.getTheater() != null ? room.getTheater().getName() : null)
                .isActive(room.getIsActive())
                .build();
    }
}
