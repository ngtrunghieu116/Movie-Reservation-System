package com.moviebooking.repository;

import com.moviebooking.model.Movie;
import com.moviebooking.model.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    @EntityGraph(attributePaths = {"genres"})
    List<Movie> findAll();

    @EntityGraph(attributePaths = {"genres"})
    Page<Movie> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"genres"})
    Optional<Movie> findById(Long id);

    @EntityGraph(attributePaths = {"genres"})
    List<Movie> findByStatus(MovieStatus status);

    @EntityGraph(attributePaths = {"genres"})
    Page<Movie> findByStatus(MovieStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"genres"})
    List<Movie> findByTitleContainingIgnoreCase(String title);

    @EntityGraph(attributePaths = {"genres"})
    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @EntityGraph(attributePaths = {"genres"})
    List<Movie> findByStatusAndTitleContainingIgnoreCase(MovieStatus status, String title);

    @EntityGraph(attributePaths = {"genres"})
    Page<Movie> findByStatusAndTitleContainingIgnoreCase(MovieStatus status, String title, Pageable pageable);

    boolean existsByTitle(String title);
    boolean existsByTitleAndIdNot(String title, Long id);
}
