package com.moviebooking.repository;

import com.moviebooking.model.Movie;
import com.moviebooking.model.enums.MovieStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    List<Movie> findByStatus(MovieStatus status);
    List<Movie> findByTitleContainingIgnoreCase(String title);
    List<Movie> findByStatusAndTitleContainingIgnoreCase(MovieStatus status, String title);
    boolean existsByTitle(String title);
    boolean existsByTitleAndIdNot(String title, Long id);
}
