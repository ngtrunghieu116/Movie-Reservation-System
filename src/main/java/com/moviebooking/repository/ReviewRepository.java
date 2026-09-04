package com.moviebooking.repository;

import com.moviebooking.model.Review;
import com.moviebooking.model.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserIdAndMovieId(Long userId, Long movieId);

    /** AI Pipeline: Trích xuất batch review theo status (phân trang, eager fetch movie) */
    @EntityGraph(attributePaths = {"movie"})
    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "movie"})
    Page<Review> findByMovieIdAndStatusOrderByCreatedAtDesc(Long movieId, ReviewStatus status, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0), COUNT(r) FROM Review r WHERE r.movie.id = :movieId AND r.status = :status")
    List<Object[]> getAverageRatingAndCount(@Param("movieId") Long movieId, @Param("status") ReviewStatus status);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.movie.id = :movieId AND r.status = :status GROUP BY r.rating")
    List<Object[]> countReviewsByRatingGroup(@Param("movieId") Long movieId, @Param("status") ReviewStatus status);

    @Query(value = "SELECT r FROM Review r JOIN FETCH r.user u JOIN FETCH r.movie m WHERE " +
            "(:movieId IS NULL OR m.id = :movieId) AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(r) FROM Review r WHERE " +
            "(:movieId IS NULL OR r.movie.id = :movieId) AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:search IS NULL OR LOWER(r.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(r.user.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(r.user.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(r.movie.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Review> searchReviewsForAdmin(@Param("movieId") Long movieId,
                                       @Param("status") ReviewStatus status,
                                       @Param("search") String search,
                                       Pageable pageable);
}
