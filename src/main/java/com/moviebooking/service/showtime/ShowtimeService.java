package com.moviebooking.service.showtime;

import com.moviebooking.context.PrimaryCinemaContext;
import com.moviebooking.dto.req.ShowtimeRequest;
import com.moviebooking.dto.res.AdminShowtimeResponse;
import com.moviebooking.dto.res.PublicShowtimeResponse;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.model.Movie;
import com.moviebooking.model.Room;
import com.moviebooking.model.Showtime;
import com.moviebooking.model.enums.MovieStatus;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ReservationRepository;
import com.moviebooking.repository.ReservedSeatRepository;
import com.moviebooking.repository.RoomRepository;
import com.moviebooking.repository.SeatRepository;
import com.moviebooking.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ShowtimeService implements IShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final RoomRepository roomRepository;
    private final MovieRepository movieRepository;
    private final ReservationRepository reservationRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final SeatRepository seatRepository;
    private final PrimaryCinemaContext primaryCinemaContext;

    @Value("${app.showtime.buffer-time-minutes:15}")
    private int bufferTimeMinutes;


    @Override
    @Transactional
    public AdminShowtimeResponse createShowtime(ShowtimeRequest request) {
        // Use Pessimistic Locking to prevent concurrent showtime creation for the same room
        Room room = roomRepository.findByIdWithLock(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chiếu"));

        if (!room.getIsActive()) {
            throw new IllegalArgumentException("Phòng chiếu đang bị vô hiệu hóa");
        }

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim"));

        validateMovieStatusForShowtime(movie);

        LocalDateTime startTime = request.getStartTime();

        validateMovieDates(movie, startTime);

        LocalDateTime endTime = startTime.plusMinutes(movie.getDuration());

        validateOverlap(room.getId(), startTime, endTime, null);

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(startTime)
                .endTime(endTime)
                .priceStandard(request.getPriceStandard())
                .priceVip(request.getPriceVip())
                .priceCouple(request.getPriceCouple())
                .build();

        Showtime saved = showtimeRepository.save(showtime);

        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminShowtimeResponse updateShowtime(Long id, ShowtimeRequest request) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu"));

        boolean hasReservations = reservationRepository.existsByShowtimeId(id);

        if (hasReservations) {
            // Cannot change room, movie, startTime, endTime
            if (!showtime.getRoom().getId().equals(request.getRoomId()) ||
                    !showtime.getMovie().getId().equals(request.getMovieId()) ||
                    !showtime.getStartTime().equals(request.getStartTime())) {
                throw new IllegalArgumentException(
                        "Không thể thay đổi phim, phòng chiếu hoặc giờ chiếu do đã có vé được đặt!");
            }
        } else {
            // Pessimistic Lock on Room when updating room/time
            Room room = roomRepository.findByIdWithLock(request.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chiếu"));

            if (!room.getIsActive()) {
                throw new IllegalArgumentException("Phòng chiếu đang bị vô hiệu hóa");
            }

            Movie movie = movieRepository.findById(request.getMovieId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim"));

            validateMovieStatusForShowtime(movie);

            LocalDateTime startTime = request.getStartTime();

            validateMovieDates(movie, startTime);

            // Note: Duration is read from current movie value to calculate end time.
            LocalDateTime endTime = startTime.plusMinutes(movie.getDuration());

            validateOverlap(room.getId(), startTime, endTime, id);

            showtime.setRoom(room);
            showtime.setMovie(movie);
            showtime.setStartTime(startTime);
            showtime.setEndTime(endTime);
        }

        // We can always update prices
        showtime.setPriceStandard(request.getPriceStandard());
        showtime.setPriceVip(request.getPriceVip());
        showtime.setPriceCouple(request.getPriceCouple());

        Showtime saved = showtimeRepository.save(showtime);

        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public void deleteShowtime(Long id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu"));

        if (reservationRepository.existsByShowtimeId(id)) {
            throw new IllegalArgumentException("Không thể xóa suất chiếu này do đã có vé được đặt!");
        }

        showtimeRepository.delete(showtime);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminShowtimeResponse getShowtimeById(Long id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu"));
        return toAdminResponse(showtime);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminShowtimeResponse> getShowtimesByRoom(Long roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return showtimeRepository.findByRoomIdOrderByStartTimeDesc(roomId, pageable)
                .map(this::toAdminResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminShowtimeResponse> searchShowtimes(Long theaterId, Long roomId, Long movieId,
            LocalDateTime fromDate, LocalDateTime toDate, int page, int size) {
        Long targetTheaterId = theaterId != null ? theaterId : primaryCinemaContext.getPrimaryTheaterId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
        return showtimeRepository.searchShowtimes(targetTheaterId, roomId, movieId, fromDate, toDate, pageable)
                .map(this::toAdminResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PublicShowtimeResponse> searchPublicShowtimes(Long theaterId, Long roomId, Long movieId,
            LocalDateTime fromDate, LocalDateTime toDate, int page, int size) {
        Long targetTheaterId = theaterId != null ? theaterId : primaryCinemaContext.getPrimaryTheaterId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
        return showtimeRepository.searchShowtimes(targetTheaterId, roomId, movieId, fromDate, toDate, pageable)
                .map(PublicShowtimeResponse::fromEntity);
    }


    private void validateMovieStatusForShowtime(Movie movie) {
        if (movie.getStatus() == MovieStatus.COMING_SOON) {
            throw new IllegalArgumentException("Không thể tạo suất chiếu cho phim đang ở trạng thái Sắp chiếu (COMING_SOON).");
        }
        if (movie.getStatus() == MovieStatus.ENDED) {
            throw new IllegalArgumentException("Không thể tạo suất chiếu cho phim đã kết thúc (ENDED).");
        }
    }

    private void validateOverlap(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId) {
        LocalDateTime adjustedStartTime = startTime.minusMinutes(bufferTimeMinutes);
        LocalDateTime adjustedEndTime = endTime.plusMinutes(bufferTimeMinutes);

        boolean isOverlap = showtimeRepository.existsOverlappingShowtime(roomId, adjustedStartTime, adjustedEndTime,
                excludeId);
        if (isOverlap) {
            throw new IllegalArgumentException("Khung giờ này đã có suất chiếu khác trong phòng (bao gồm "
                    + bufferTimeMinutes + " phút dọn phòng)!");
        }
    }

    private void validateMovieDates(Movie movie, LocalDateTime startTime) {
        java.time.LocalDate showDate = startTime.toLocalDate();
        if (movie.getReleaseDate() != null && showDate.isBefore(movie.getReleaseDate())) {
            throw new IllegalArgumentException(
                    String.format("Không thể tạo suất chiếu trước ngày công chiếu của phim (%s).", movie.getReleaseDate()));
        }
        if (movie.getEndDate() != null && showDate.isAfter(movie.getEndDate())) {
            throw new IllegalArgumentException(
                    String.format("Không thể tạo suất chiếu sau ngày kết thúc của phim (%s).", movie.getEndDate()));
        }
    }

    private AdminShowtimeResponse toAdminResponse(Showtime showtime) {
        long totalSeats = seatRepository.countByRoomId(showtime.getRoom().getId());
        long bookedSeats = reservedSeatRepository.countBookedSeatsByShowtimeId(showtime.getId());
        long availableSeats = totalSeats - bookedSeats;

        return AdminShowtimeResponse.fromEntity(showtime, bookedSeats, availableSeats);
    }
}
