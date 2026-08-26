package com.moviebooking.crawler.resolver;

import com.moviebooking.model.Room;
import com.moviebooking.model.Theater;
import com.moviebooking.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomResolverTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomResolver roomResolver;

    private Room createRoom(Long id, String name, boolean isActive, String sourceRoomId) {
        Theater theater = Theater.builder().id(1L).name("CineMind").build();
        return Room.builder()
                .id(id)
                .name(name)
                .isActive(isActive)
                .sourceRoomId(sourceRoomId)
                .theater(theater)
                .build();
    }

    @Test
    void resolve_WithMappedActiveRoom_ShouldReturnResolved() {
        Room room = createRoom(1L, "Phòng chiếu 1", true, "2100");
        when(roomRepository.findBySourceRoomIdAndTheaterId("2100", 1L)).thenReturn(Optional.of(room));

        RoomResolver.RoomResolveResult result = roomResolver.resolve("2100", 1L);

        assertEquals(RoomResolver.Status.RESOLVED, result.status());
        assertNotNull(result.room());
        assertEquals(1L, result.room().getId());
    }

    @Test
    void resolve_WithUnmappedRoom_ShouldReturnUnmapped() {
        when(roomRepository.findBySourceRoomIdAndTheaterId("9999", 1L)).thenReturn(Optional.empty());

        RoomResolver.RoomResolveResult result = roomResolver.resolve("9999", 1L);

        assertEquals(RoomResolver.Status.UNMAPPED_ROOM, result.status());
        assertNull(result.room());
    }

    @Test
    void resolve_WithMaintenanceRoom_ShouldReturnMaintenance() {
        Room room = createRoom(2L, "Phòng chiếu 2", false, "2114");
        when(roomRepository.findBySourceRoomIdAndTheaterId("2114", 1L)).thenReturn(Optional.of(room));

        RoomResolver.RoomResolveResult result = roomResolver.resolve("2114", 1L);

        assertEquals(RoomResolver.Status.MAINTENANCE_ROOM, result.status());
        assertNull(result.room());
    }

    @Test
    void resolve_WithNullRoomId_ShouldReturnUnmapped() {
        RoomResolver.RoomResolveResult result = roomResolver.resolve(null, 1L);
        assertEquals(RoomResolver.Status.UNMAPPED_ROOM, result.status());
    }

    @Test
    void resolve_WithBlankRoomId_ShouldReturnUnmapped() {
        RoomResolver.RoomResolveResult result = roomResolver.resolve("  ", 1L);
        assertEquals(RoomResolver.Status.UNMAPPED_ROOM, result.status());
    }
}
