package com.moviebooking.repository;

import com.moviebooking.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    Optional<Genre> findByName(String name);
}
