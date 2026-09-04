package com.moviebooking.controller;

import com.moviebooking.dto.res.ArticleResponse;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.service.article.IArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class PublicArticleController {

    private final IArticleService articleService;

    @GetMapping
    public ResponseEntity<PageResponse<ArticleResponse>> getPublicArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(articleService.getPublicArticles(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getPublicArticleById(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getPublicArticleById(id));
    }
}
