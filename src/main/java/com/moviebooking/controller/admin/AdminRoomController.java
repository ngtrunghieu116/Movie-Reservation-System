package com.moviebooking.controller.admin;

import com.moviebooking.dto.req.RoomRequest;
import com.moviebooking.dto.res.RoomResponse;
import com.moviebooking.service.room.IRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
public class AdminRoomController {

    private final IRoomService roomService;

    @GetMapping
    public ResponseEntity<?> getRooms(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long theaterId,
            @RequestParam(required = false) String search) {
        if (page != null && size != null) {
            return ResponseEntity.ok(roomService.getRoomsPaged(page, size, theaterId, search));
        }
        if (theaterId != null) {
            return ResponseEntity.ok(roomService.getActiveRoomsByTheaterId(theaterId));
        }
        return ResponseEntity.ok(roomService.getRoomsPaged(0, 100, null, null).getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(Map.of("message", "Xóa phòng chiếu thành công!"));
    }
}
