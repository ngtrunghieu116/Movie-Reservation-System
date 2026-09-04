package com.moviebooking.repository;

import com.moviebooking.model.Article;
import com.moviebooking.model.enums.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    Page<Article> findByStatusOrderByCreatedAtDesc(ArticleStatus status, Pageable pageable);

    /** AI Pipeline: Trích xuất batch article theo status (phân trang) */
    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);

    Optional<Article> findByIdAndStatus(Long id, ArticleStatus status);

    @Query("SELECT a FROM Article a WHERE " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Article> searchArticles(@Param("status") ArticleStatus status,
                                 @Param("search") String search,
                                 Pageable pageable);
}
