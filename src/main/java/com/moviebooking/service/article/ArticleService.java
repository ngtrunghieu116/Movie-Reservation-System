package com.moviebooking.service.article;

import com.moviebooking.dto.req.CreateArticleRequest;
import com.moviebooking.dto.req.UpdateArticleRequest;
import com.moviebooking.dto.res.ArticleResponse;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.model.Article;
import com.moviebooking.model.enums.ArticleStatus;
import com.moviebooking.repository.ArticleRepository;
import com.moviebooking.service.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService implements IArticleService {

    private final ArticleRepository articleRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> getPublicArticles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> articlePage = articleRepository.findByStatusOrderByCreatedAtDesc(ArticleStatus.PUBLISHED, pageable);

        List<ArticleResponse> content = articlePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<ArticleResponse>builder()
                .content(content)
                .pageNo(articlePage.getNumber())
                .pageSize(articlePage.getSize())
                .totalElements(articlePage.getTotalElements())
                .totalPages(articlePage.getTotalPages())
                .last(articlePage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleResponse getPublicArticleById(Long id) {
        Article article = articleRepository.findByIdAndStatus(id, ArticleStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết hoặc bài viết chưa được xuất bản!"));
        return mapToResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> getAdminArticles(ArticleStatus status, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<Article> articlePage = articleRepository.searchArticles(status, trimmedSearch, pageable);

        List<ArticleResponse> content = articlePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<ArticleResponse>builder()
                .content(content)
                .pageNo(articlePage.getNumber())
                .pageSize(articlePage.getSize())
                .totalElements(articlePage.getTotalElements())
                .totalPages(articlePage.getTotalPages())
                .last(articlePage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleResponse getAdminArticleById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết với ID: " + id));
        return mapToResponse(article);
    }

    @Override
    @Transactional
    public ArticleResponse createArticle(CreateArticleRequest request, MultipartFile poster) {
        String posterUrl = null;
        if (poster != null && !poster.isEmpty()) {
            posterUrl = fileStorageService.storeArticlePoster(poster);
        }

        ArticleStatus initialStatus = request.getStatus() != null ? request.getStatus() : ArticleStatus.DRAFT;

        Article article = Article.builder()
                .title(request.getTitle().trim())
                .shortDescription(request.getShortDescription().trim())
                .content(request.getContent())
                .posterUrl(posterUrl)
                .status(initialStatus)
                .build();

        Article saved = articleRepository.save(article);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ArticleResponse updateArticle(Long id, UpdateArticleRequest request, MultipartFile poster) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết với ID: " + id));

        article.setTitle(request.getTitle().trim());
        article.setShortDescription(request.getShortDescription().trim());
        article.setContent(request.getContent());

        if (request.getStatus() != null) {
            article.setStatus(request.getStatus());
        }

        if (poster != null && !poster.isEmpty()) {
            String oldPosterUrl = article.getPosterUrl();
            String newPosterUrl = fileStorageService.storeArticlePoster(poster);
            article.setPosterUrl(newPosterUrl);

            if (oldPosterUrl != null && !oldPosterUrl.trim().isEmpty()) {
                fileStorageService.deleteFile(oldPosterUrl);
            }
        }

        Article updated = articleRepository.save(article);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ArticleResponse updateStatus(Long id, ArticleStatus status) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết với ID: " + id));

        article.setStatus(status);
        Article updated = articleRepository.save(article);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết với ID: " + id));

        String posterUrl = article.getPosterUrl();
        articleRepository.delete(article);

        if (posterUrl != null && !posterUrl.trim().isEmpty()) {
            fileStorageService.deleteFile(posterUrl);
        }
    }

    private ArticleResponse mapToResponse(Article article) {
        return ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .shortDescription(article.getShortDescription())
                .content(article.getContent())
                .posterUrl(article.getPosterUrl())
                .status(article.getStatus())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}
