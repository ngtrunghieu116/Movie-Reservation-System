package com.moviebooking.service.seat;

import com.moviebooking.dto.req.BatchGenerateSeatsRequest;
import com.moviebooking.dto.req.BatchUpdateSeatsRequest;
import com.moviebooking.dto.req.UpdateSeatRequest;
import com.moviebooking.dto.res.SeatResponse;
import com.moviebooking.model.Room;
import com.moviebooking.model.Seat;
import com.moviebooking.model.enums.SeatType;
import com.moviebooking.repository.RoomRepository;
import com.moviebooking.repository.SeatRepository;
import com.moviebooking.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService implements ISeatService {

    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;
    private final ShowtimeRepository showtimeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByRoomId(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new IllegalArgumentException("Không tìm thấy phòng chiếu với ID: " + roomId);
        }
        return seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(roomId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<SeatResponse> generateSeatLayout(Long roomId, BatchGenerateSeatsRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu với ID: " + roomId));

        // Rule 2: Validation - Không cho sửa nếu phòng đã có lịch chiếu sắp tới
        if (showtimeRepository.existsByRoomIdAndEndTimeAfter(roomId, LocalDateTime.now())) {
            throw new IllegalStateException("Không thể chỉnh sửa hoặc sinh lại sơ đồ ghế vì phòng chiếu đang có lịch chiếu sắp diễn ra!");
        }

        // Rule 1: Validation - Không ghi đè nếu chưa được xác nhận
        boolean hasExistingSeats = seatRepository.existsByRoomId(roomId);
        if (hasExistingSeats && !Boolean.TRUE.equals(request.getOverrideExisting())) {
            throw new IllegalStateException("Phòng chiếu đã có sơ đồ ghế. Vui lòng xác nhận ghi đè toàn bộ nếu muốn khởi tạo lại!");
        }

        // Xóa sơ đồ cũ nếu override
        if (hasExistingSeats) {
            seatRepository.deleteByRoomId(roomId);
            seatRepository.flush();
        }

        // Parse startRow và endRow
        char start = request.getStartRow().trim().toUpperCase().charAt(0);
        char end = request.getEndRow().trim().toUpperCase().charAt(0);

        if (start > end) {
            throw new IllegalArgumentException("Hàng bắt đầu không được vượt quá hàng kết thúc!");
        }

        List<Seat> newSeats = new ArrayList<>();
        SeatType defaultType = request.getDefaultSeatType() != null ? request.getDefaultSeatType() : SeatType.STANDARD;

        for (char r = start; r <= end; r++) {
            String rowName = String.valueOf(r);
            SeatType rowType = defaultType;
            if (request.getRowSeatTypes() != null && request.getRowSeatTypes().containsKey(rowName)) {
                rowType = request.getRowSeatTypes().get(rowName);
            }

            for (int number = 1; number <= request.getSeatsPerRow(); number++) {
                Seat seat = Seat.builder()
                        .room(room)
                        .rowName(rowName)
                        .seatNumber(number)
                        .seatType(rowType)
                        .isActive(true)
                        .build();
                newSeats.add(seat);
            }
        }

        List<Seat> savedSeats = seatRepository.saveAll(newSeats);
        log.info("Successfully generated {} seats for room ID {}", savedSeats.size(), roomId);

        return savedSeats.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SeatResponse updateSeat(Long seatId, UpdateSeatRequest request) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế với ID: " + seatId));

        if (showtimeRepository.existsByRoomIdAndEndTimeAfter(seat.getRoom().getId(), LocalDateTime.now())) {
            throw new IllegalStateException("Không thể chỉnh sửa ghế vì phòng chiếu đang có lịch chiếu sắp diễn ra!");
        }

        if (request.getSeatType() != null) {
            seat.setSeatType(request.getSeatType());
        }
        if (request.getIsActive() != null) {
            seat.setIsActive(request.getIsActive());
        }

        Seat savedSeat = seatRepository.save(seat);
        return mapToResponse(savedSeat);
    }

    @Override
    @Transactional
    public List<SeatResponse> batchUpdateSeats(BatchUpdateSeatsRequest request) {
        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        if (seats.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy ghế nào phù hợp với danh sách ID đã chọn!");
        }

        Long roomId = seats.get(0).getRoom().getId();
        if (showtimeRepository.existsByRoomIdAndEndTimeAfter(roomId, LocalDateTime.now())) {
            throw new IllegalStateException("Không thể chỉnh sửa ghế vì phòng chiếu đang có lịch chiếu sắp diễn ra!");
        }

        for (Seat seat : seats) {
            if (request.getSeatType() != null) {
                seat.setSeatType(request.getSeatType());
            }
            if (request.getIsActive() != null) {
                seat.setIsActive(request.getIsActive());
            }
        }

        List<Seat> updatedSeats = seatRepository.saveAll(seats);
        return updatedSeats.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private SeatResponse mapToResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .rowName(seat.getRowName())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .isActive(seat.getIsActive())
                .roomId(seat.getRoom().getId())
                .build();
    }
}
