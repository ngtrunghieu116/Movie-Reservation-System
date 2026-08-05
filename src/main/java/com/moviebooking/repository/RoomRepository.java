package com.moviebooking.repository;

import com.moviebooking.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByNameAndTheaterId(String name, Long theaterId);
    boolean existsByNameAndTheaterIdAndIdNot(String name, Long theaterId, Long id);

    @EntityGraph(attributePaths = {"theater"})
    List<Room> findAll();

    @EntityGraph(attributePaths = {"theater"})
    Page<Room> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"theater"})
    Optional<Room> findById(Long id);

    @EntityGraph(attributePaths = {"theater"})
    List<Room> findByTheaterId(Long theaterId);

    @EntityGraph(attributePaths = {"theater"})
    Page<Room> findByTheaterId(Long theaterId, Pageable pageable);

    @EntityGraph(attributePaths = {"theater"})
    Page<Room> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = {"theater"})
    Page<Room> findByTheaterIdAndNameContainingIgnoreCase(Long theaterId, String name, Pageable pageable);

    @EntityGraph(attributePaths = {"theater"})
    List<Room> findByTheaterIdAndIsActiveTrue(Long theaterId);
}
