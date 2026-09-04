package com.moviebooking.service.article;

import com.moviebooking.dto.req.CreateArticleRequest;
import com.moviebooking.dto.req.UpdateArticleRequest;
import com.moviebooking.dto.res.ArticleResponse;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.model.enums.ArticleStatus;
import org.springframework.web.multipart.MultipartFile;

public interface IArticleService {
    PageResponse<ArticleResponse> getPublicArticles(int page, int size);
    ArticleResponse getPublicArticleById(Long id);

    PageResponse<ArticleResponse> getAdminArticles(ArticleStatus status, String search, int page, int size);
    ArticleResponse getAdminArticleById(Long id);
    ArticleResponse createArticle(CreateArticleRequest request, MultipartFile poster);
    ArticleResponse updateArticle(Long id, UpdateArticleRequest request, MultipartFile poster);
    ArticleResponse updateStatus(Long id, ArticleStatus status);
    void deleteArticle(Long id);
}
