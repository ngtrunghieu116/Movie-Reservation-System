package com.moviebooking.service.theater;

import com.moviebooking.dto.req.TheaterRequest;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.dto.res.TheaterResponse;
import com.moviebooking.model.Theater;
import com.moviebooking.repository.RoomRepository;
import com.moviebooking.repository.SeatRepository;
import com.moviebooking.repository.ShowtimeRepository;
import com.moviebooking.repository.TheaterRepository;
import com.moviebooking.model.Room;
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
public class TheaterService implements ITheaterService {

    private final TheaterRepository theaterRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TheaterResponse> getAllActiveTheaters() {
        return theaterRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TheaterResponse> getAllTheaters() {
        return theaterRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TheaterResponse> getTheatersPaged(int pageNo, int pageSize, String search) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        Page<Theater> theaterPage;

        if (search != null && !search.trim().isEmpty()) {
            theaterPage = theaterRepository.findByNameContainingIgnoreCaseOrCityContainingIgnoreCase(search.trim(),
                    search.trim(), pageable);
        } else {
            theaterPage = theaterRepository.findAll(pageable);
        }

        List<TheaterResponse> content = theaterPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<TheaterResponse>builder()
                .content(content)
                .pageNo(theaterPage.getNumber())
                .pageSize(theaterPage.getSize())
                .totalElements(theaterPage.getTotalElements())
                .totalPages(theaterPage.getTotalPages())
                .last(theaterPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TheaterResponse getTheaterById(Long id) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy rạp/cơ sở với ID: " + id));
        return mapToResponse(theater);
    }

    @Override
    @Transactional
    public TheaterResponse createTheater(TheaterRequest request) {
        if (theaterRepository.existsByName(request.getName())) {
            throw new RuntimeException("Cơ sở này đã tồn tại trong hệ thống!");
        }

        Theater theater = Theater.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .district(request.getDistrict())
                .phone(request.getPhone())
                .email(request.getEmail())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Theater savedTheater = theaterRepository.save(theater);
        return mapToResponse(savedTheater);
    }

    @Override
    @Transactional
    public TheaterResponse updateTheater(Long id, TheaterRequest request) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy rạp/cơ sở với ID: " + id));

        if (theaterRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new RuntimeException("Cơ sở này đã trùng với một cơ sở khác!");
        }

        theater.setName(request.getName());
        theater.setAddress(request.getAddress());
        theater.setCity(request.getCity());
        theater.setDistrict(request.getDistrict());
        theater.setPhone(request.getPhone());
        theater.setEmail(request.getEmail());
        theater.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            theater.setIsActive(request.getIsActive());
        }

        Theater updatedTheater = theaterRepository.save(theater);
        return mapToResponse(updatedTheater);
    }

    @Override
    @Transactional
    public void deleteTheater(Long id) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy rạp/cơ sở với ID: " + id));

        List<Room> rooms = roomRepository.findByTheaterId(id);
        for (Room room : rooms) {
            if (showtimeRepository.existsByRoomIdAndEndTimeAfter(room.getId(), LocalDateTime.now())) {
                throw new IllegalStateException("Không thể xóa cơ sở rạp vì có phòng chiếu đang chứa lịch chiếu sắp diễn ra!");
            }
            seatRepository.deleteByRoomId(room.getId());
        }
        seatRepository.flush();
        if (!rooms.isEmpty()) {
            roomRepository.deleteAll(rooms);
            roomRepository.flush();
        }
        theaterRepository.delete(theater);
    }

    private TheaterResponse mapToResponse(Theater theater) {
        return TheaterResponse.builder()
                .id(theater.getId())
                .name(theater.getName())
                .address(theater.getAddress())
                .city(theater.getCity())
                .district(theater.getDistrict())
                .phone(theater.getPhone())
                .email(theater.getEmail())
                .description(theater.getDescription())
                .isActive(theater.getIsActive())
                .build();
    }
}
