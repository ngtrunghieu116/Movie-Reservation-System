package com.moviebooking.repository;

import com.moviebooking.model.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    boolean existsByRoomIdAndEndTimeAfter(Long roomId, LocalDateTime time);

    @Query("SELECT COUNT(s) > 0 FROM Showtime s WHERE s.room.id = :roomId " +
           "AND :adjustedStartTime < s.endTime AND :adjustedEndTime > s.startTime " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId)")
    boolean existsOverlappingShowtime(@Param("roomId") Long roomId,
                                      @Param("adjustedStartTime") LocalDateTime adjustedStartTime,
                                      @Param("adjustedEndTime") LocalDateTime adjustedEndTime,
                                      @Param("excludeId") Long excludeId);

    Page<Showtime> findByRoomIdOrderByStartTimeDesc(Long roomId, Pageable pageable);
    
    @Query(value = "SELECT s FROM Showtime s JOIN FETCH s.movie JOIN FETCH s.room r JOIN FETCH r.theater WHERE " +
           "(:theaterId IS NULL OR r.theater.id = :theaterId) AND " +
           "(:roomId IS NULL OR r.id = :roomId) AND " +
           "(:movieId IS NULL OR s.movie.id = :movieId) AND " +
           "(:fromDate IS NULL OR s.startTime >= :fromDate) AND " +
           "(:toDate IS NULL OR s.startTime <= :toDate)",
           countQuery = "SELECT COUNT(s) FROM Showtime s WHERE " +
           "(:theaterId IS NULL OR s.room.theater.id = :theaterId) AND " +
           "(:roomId IS NULL OR s.room.id = :roomId) AND " +
           "(:movieId IS NULL OR s.movie.id = :movieId) AND " +
           "(:fromDate IS NULL OR s.startTime >= :fromDate) AND " +
           "(:toDate IS NULL OR s.startTime <= :toDate)")
    Page<Showtime> searchShowtimes(@Param("theaterId") Long theaterId,
                                   @Param("roomId") Long roomId,
                                   @Param("movieId") Long movieId,
                                   @Param("fromDate") LocalDateTime fromDate,
                                   @Param("toDate") LocalDateTime toDate,
                                   Pageable pageable);
}
