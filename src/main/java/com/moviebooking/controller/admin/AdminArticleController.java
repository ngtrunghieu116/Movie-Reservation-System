package com.moviebooking.controller.admin;

import com.moviebooking.dto.req.CreateArticleRequest;
import com.moviebooking.dto.req.UpdateArticleRequest;
import com.moviebooking.dto.res.ArticleResponse;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.model.enums.ArticleStatus;
import com.moviebooking.service.article.IArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final IArticleService articleService;

    @GetMapping
    public ResponseEntity<PageResponse<ArticleResponse>> getArticles(
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(articleService.getAdminArticles(status, search, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticleById(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getAdminArticleById(id));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ArticleResponse> createArticle(
            @Valid @ModelAttribute CreateArticleRequest request,
            @RequestParam(value = "poster", required = false) MultipartFile poster) {
        return new ResponseEntity<>(articleService.createArticle(request, poster), HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateArticleRequest request,
            @RequestParam(value = "poster", required = false) MultipartFile poster) {
        return ResponseEntity.ok(articleService.updateArticle(id, request, poster));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<ArticleResponse> publishArticle(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.updateStatus(id, ArticleStatus.PUBLISHED));
    }

    @PatchMapping("/{id}/hide")
    public ResponseEntity<ArticleResponse> hideArticle(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.updateStatus(id, ArticleStatus.HIDDEN));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}
