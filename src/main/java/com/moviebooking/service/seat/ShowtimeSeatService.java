package com.moviebooking.service.seat;

import com.moviebooking.dto.req.HoldSeatsRequest;
import com.moviebooking.dto.req.ReleaseSeatsRequest;
import com.moviebooking.dto.res.HoldSeatsResponse;
import com.moviebooking.dto.res.PublicShowtimeSeatResponse;
import com.moviebooking.exception.*;
import com.moviebooking.model.*;
import com.moviebooking.model.enums.SeatType;
import com.moviebooking.model.enums.ShowtimeSeatStatus;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeSeatService {

    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;

    @Value("${booking.seat-hold-minutes:8}")
    private int seatHoldMinutes = 8;

    /**
     * Initializes ShowtimeSeat inventory for a given Showtime.
     * IDEMPOTENT: Existing seats are NEVER recreated or modified (preserving price snapshot & status).
     */
    @Transactional
    public void initializeSeatsForShowtime(Showtime showtime) {
        if (showtime == null || showtime.getRoom() == null) {
            return;
        }

        List<Seat> physicalSeats = seatRepository.findByRoomIdOrderByRowNameAscSeatNumberAsc(showtime.getRoom().getId());
        List<ShowtimeSeat> existingSeats = showtimeSeatRepository.findByShowtimeIdOrderBySeatRowNameAscSeatSeatNumberAsc(showtime.getId());

        Map<Long, ShowtimeSeat> existingSeatMap = existingSeats.stream()
                .collect(Collectors.toMap(ss -> ss.getSeat().getId(), Function.identity(), (s1, s2) -> s1));

        List<ShowtimeSeat> seatsToCreate = new ArrayList<>();

        for (Seat seat : physicalSeats) {
            if (!existingSeatMap.containsKey(seat.getId())) {
                BigDecimal snapshotPrice = resolvePriceForSeatType(showtime, seat.getSeatType());
                ShowtimeSeat newShowtimeSeat = ShowtimeSeat.builder()
                        .showtime(showtime)
                        .seat(seat)
                        .status(ShowtimeSeatStatus.AVAILABLE)
                        .price(snapshotPrice)
                        .build();
                seatsToCreate.add(newShowtimeSeat);
            }
        }

        if (!seatsToCreate.isEmpty()) {
            showtimeSeatRepository.saveAll(seatsToCreate);
            log.info("[SHOWTIME_SEAT_INIT] Initialized {} new ShowtimeSeats for showtimeId={}", seatsToCreate.size(), showtime.getId());
        }
    }

    /**
     * Admin/migration helper method to backfill ShowtimeSeat records for older showtimes.
     */
    @Transactional
    public void backfillSeatsForShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu ID: " + showtimeId));
        initializeSeatsForShowtime(showtime);
    }

    /**
     * Fetches public seat map for client.
     * READ-ONLY: MUST NOT mutate database. Expirations are computed dynamically in DTO.
     */
    @Transactional(readOnly = true)
    public List<PublicShowtimeSeatResponse> getPublicSeatMap(Long showtimeId) {
        if (!showtimeRepository.existsById(showtimeId)) {
            throw new ResourceNotFoundException("Không tìm thấy suất chiếu ID: " + showtimeId);
        }

        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeIdOrderBySeatRowNameAscSeatSeatNumberAsc(showtimeId);
        return seats.stream()
                .map(PublicShowtimeSeatResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Holds seats temporarily for 8 minutes using Pessimistic Write Lock.
     */
    @Transactional
    public HoldSeatsResponse holdSeats(HoldSeatsRequest request, User currentUser) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new InvalidSeatHoldException("Danh sách seatIds không được để trống");
        }

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu ID: " + request.getShowtimeId()));

        // Validate showtime bookability
        if (Boolean.FALSE.equals(showtime.getIsActive())) {
            throw new ShowtimeNotBookableException("Suất chiếu hiện đang bị vô hiệu hóa");
        }
        if (Boolean.FALSE.equals(showtime.getIsOnlineSelling())) {
            throw new ShowtimeNotBookableException("Suất chiếu tạm dừng bán vé trực tuyến");
        }
        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ShowtimeNotBookableException("Suất chiếu đã bắt đầu hoặc đã qua giờ chiếu");
        }

        // Clean & sort seat IDs ascending for deterministic lock ordering (prevent deadlock)
        List<Long> sortedSeatIds = request.getSeatIds().stream()
                .distinct()
                .sorted()
                .toList();

        // Acquire Pessimistic Write Lock
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtime.getId(), sortedSeatIds);

        if (seats.size() != sortedSeatIds.size()) {
            throw new InvalidSeatHoldException("Một số ghế yêu cầu không tồn tại trong suất chiếu này");
        }

        LocalDateTime now = LocalDateTime.now();

        // Check availability of each seat
        for (ShowtimeSeat ss : seats) {
            ShowtimeSeatStatus currentStatus = ss.getStatus();
            if (currentStatus == ShowtimeSeatStatus.SOLD) {
                throw new SeatAlreadyReservedException("Ghế " + ss.getSeat().getRowName() + ss.getSeat().getSeatNumber() + " đã được bán");
            }
            if (currentStatus == ShowtimeSeatStatus.HELD) {
                if (ss.getLockedUntil() != null && ss.getLockedUntil().isAfter(now)) {
                    throw new SeatAlreadyReservedException("Ghế " + ss.getSeat().getRowName() + ss.getSeat().getSeatNumber() + " đang được người khác giữ chỗ");
                }
            }
        }

        // All seats are available or expired-held. Lock them now atomically.
        String holdToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = now.plusMinutes(seatHoldMinutes);

        for (ShowtimeSeat ss : seats) {
            ss.setStatus(ShowtimeSeatStatus.HELD);
            ss.setHoldToken(holdToken);
            ss.setHeldByUser(currentUser);
            ss.setLockedUntil(expiresAt);
        }

        showtimeSeatRepository.saveAll(seats);

        List<PublicShowtimeSeatResponse> heldSeatDTOs = seats.stream()
                .map(ss -> PublicShowtimeSeatResponse.builder()
                        .seatId(ss.getSeat().getId())
                        .rowName(ss.getSeat().getRowName())
                        .seatNumber(ss.getSeat().getSeatNumber())
                        .seatType(ss.getSeat().getSeatType())
                        .price(ss.getPrice())
                        .status(ShowtimeSeatStatus.HELD)
                        .build())
                .collect(Collectors.toList());

        return HoldSeatsResponse.builder()
                .showtimeId(showtime.getId())
                .holdToken(holdToken)
                .heldSeats(heldSeatDTOs)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * Releases held seats by the owner.
     */
    @Transactional
    public void releaseSeats(ReleaseSeatsRequest request, User currentUser) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new InvalidSeatHoldException("Danh sách seatIds không được để trống");
        }
        if (request.getHoldToken() == null || request.getHoldToken().isBlank()) {
            throw new InvalidSeatHoldException("holdToken không được để trống");
        }

        // Clean & sort seat IDs ascending for deterministic lock ordering
        List<Long> sortedSeatIds = request.getSeatIds().stream()
                .distinct()
                .sorted()
                .toList();

        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(request.getShowtimeId(), sortedSeatIds);

        if (seats.size() != sortedSeatIds.size()) {
            throw new InvalidSeatHoldException("Một số ghế yêu cầu không tồn tại trong suất chiếu này");
        }

        LocalDateTime now = LocalDateTime.now();

        // Validate ownership and status of ALL seats before releasing any
        for (ShowtimeSeat ss : seats) {
            if (ss.getStatus() != ShowtimeSeatStatus.HELD) {
                throw new InvalidSeatHoldException("Ghế " + ss.getSeat().getRowName() + ss.getSeat().getSeatNumber() + " không ở trạng thái giữ chỗ");
            }
            if (!request.getHoldToken().equals(ss.getHoldToken())) {
                throw new InvalidSeatHoldException("Mã holdToken không khớp cho ghế " + ss.getSeat().getRowName() + ss.getSeat().getSeatNumber());
            }
            if (ss.getHeldByUser() == null || !ss.getHeldByUser().getId().equals(currentUser.getId())) {
                throw new SeatHoldOwnershipException("Bạn không có quyền giải phóng ghế " + ss.getSeat().getRowName() + ss.getSeat().getSeatNumber());
            }
            if (ss.getLockedUntil() == null || !ss.getLockedUntil().isAfter(now)) {
                throw new InvalidSeatHoldException("Thời gian giữ ghế " + ss.getSeat().getRowName() + ss.getSeat().getSeatNumber() + " đã hết hạn");
            }
        }

        // All seats are valid. Release them atomically.
        for (ShowtimeSeat ss : seats) {
            ss.setStatus(ShowtimeSeatStatus.AVAILABLE);
            ss.setHoldToken(null);
            ss.setHeldByUser(null);
            ss.setLockedUntil(null);
        }

        showtimeSeatRepository.saveAll(seats);
    }

    private BigDecimal resolvePriceForSeatType(Showtime showtime, SeatType seatType) {
        if (seatType == null) return showtime.getPriceStandard();
        return switch (seatType) {
            case VIP -> showtime.getPriceVip() != null ? showtime.getPriceVip() : showtime.getPriceStandard();
            case COUPLE -> showtime.getPriceCouple() != null ? showtime.getPriceCouple() : showtime.getPriceStandard();
            default -> showtime.getPriceStandard();
        };
    }
}
