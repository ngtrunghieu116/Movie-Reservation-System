package com.moviebooking.service.ai.pipeline.extractor;

import com.moviebooking.dto.ai.KnowledgeDocument;
import com.moviebooking.model.Movie;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.service.ai.pipeline.transformer.DocumentTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Trích xuất dữ liệu phim và chuyển thành KnowledgeDocument.
 * Sử dụng EntityGraph đã có trong MovieRepository để nạp genres hiệu quả.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MovieDocumentExtractor {

    private final MovieRepository movieRepository;
    private final DocumentTransformer transformer;

    /**
     * Trích xuất tất cả phim theo batch phân trang.
     */
    public List<KnowledgeDocument> extractAll(int batchSize) {
        List<KnowledgeDocument> documents = new ArrayList<>();
        int page = 0;

        Page<Movie> moviePage;
        do {
            moviePage = movieRepository.findAll(PageRequest.of(page, batchSize));
            for (Movie movie : moviePage.getContent()) {
                try {
                    documents.add(transformer.transformMovie(movie));
                } catch (Exception e) {
                    log.error("Failed to transform movie id={}: {}", movie.getId(), e.getMessage());
                }
            }
            page++;
        } while (moviePage.hasNext());

        log.info("Extracted {} movie documents", documents.size());
        return documents;
    }

    /**
     * Trích xuất một phim theo ID.
     */
    public Optional<KnowledgeDocument> extractById(Long movieId) {
        return movieRepository.findById(movieId)
                .map(transformer::transformMovie);
    }
}
