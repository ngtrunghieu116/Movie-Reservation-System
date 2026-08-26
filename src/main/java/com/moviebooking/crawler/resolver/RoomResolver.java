package com.moviebooking.crawler.resolver;

import com.moviebooking.model.Room;
import com.moviebooking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Resolves NCC RoomId to CineMind Room entity via source_room_id mapping.
 *
 * Business Rules:
 * - RULE-07: source_room_id is UNIQUE per Theater. Unknown NCC RoomId → SKIP + LOG UNMAPPED_ROOM.
 * - RULE-08: Room in MAINTENANCE (isActive == false) → SKIP + LOG SKIP_MAINTENANCE_ROOM.
 * - No random mapping, no auto-create, no default room.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomResolver {

    private final RoomRepository roomRepository;

    public enum Status {
        RESOLVED,
        UNMAPPED_ROOM,
        MAINTENANCE_ROOM
    }

    public record RoomResolveResult(Status status, Room room) {}

    /**
     * Resolves a NCC RoomId to a CineMind Room.
     *
     * @param nccRoomId The raw RoomId from NCC payload (e.g. "2114")
     * @param theaterId The CineMind theater ID to scope the lookup (default: 1)
     * @return RoomResolveResult with status and resolved Room (null if not resolved)
     */
    @Transactional(readOnly = true)
    public RoomResolveResult resolve(String nccRoomId, Long theaterId) {
        if (nccRoomId == null || nccRoomId.isBlank()) {
            log.warn("[UNMAPPED_ROOM] NCC RoomId is null or blank");
            return new RoomResolveResult(Status.UNMAPPED_ROOM, null);
        }

        Optional<Room> roomOpt = roomRepository.findBySourceRoomIdAndTheaterId(nccRoomId, theaterId);

        if (roomOpt.isEmpty()) {
            log.warn("[UNMAPPED_ROOM] No CineMind Room mapped for NCC RoomId={} theaterId={}", nccRoomId, theaterId);
            return new RoomResolveResult(Status.UNMAPPED_ROOM, null);
        }

        Room room = roomOpt.get();

        if (!room.getIsActive()) {
            log.warn("[SKIP_MAINTENANCE_ROOM] CineMind Room id={} name='{}' is in MAINTENANCE for NCC RoomId={}",
                    room.getId(), room.getName(), nccRoomId);
            return new RoomResolveResult(Status.MAINTENANCE_ROOM, null);
        }

        return new RoomResolveResult(Status.RESOLVED, room);
    }
}
