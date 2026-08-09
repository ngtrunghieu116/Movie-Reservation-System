package com.moviebooking.crawler.orchestrator;

import com.moviebooking.crawler.client.CrawlerClient;
import com.moviebooking.crawler.dto.MovieDetailDTO;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import com.moviebooking.crawler.enricher.MovieEnricher;
import com.moviebooking.crawler.mapper.MovieMapper;
import com.moviebooking.crawler.validator.BusinessValidator;
import com.moviebooking.crawler.validator.DtoValidator;
import com.moviebooking.model.Movie;
import com.moviebooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerOrchestrator {

    private final CrawlerClient crawlerClient;
    private final DtoValidator dtoValidator;
    private final BusinessValidator businessValidator;
    private final MovieMapper movieMapper;
    private final MovieEnricher movieEnricher;
    private final MovieRepository movieRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processMovie(MovieListItemDTO listItem) {
        log.info("[INFO] Processing movie title={} sourceId={}", listItem.getTitle(), listItem.getSourceId());
        
        try {
            // 1. Validate DTO
            dtoValidator.validateListItem(listItem);

            // 2. Fetch Detail early for both new insertion and missing data enrichment
            MovieDetailDTO detail = null;
            if (listItem.getDetailUrl() != null && !listItem.getDetailUrl().isBlank()) {
                detail = crawlerClient.fetchMovieDetail(listItem.getDetailUrl());
                dtoValidator.validateDetail(detail, listItem.getTitle());
            }

            // 3. Check Duplicate
            Optional<Movie> existingOpt = movieRepository.findBySourceId(listItem.getSourceId());
            if (existingOpt.isPresent()) {
                Movie existingMovie = existingOpt.get();
                // Check if existing movie has missing genres that can be supplemented now
                boolean genresSupplemented = movieEnricher.enrichMissingGenres(existingMovie, detail);
                if (genresSupplemented) {
                    movieRepository.save(existingMovie);
                    log.info("[INFO] Supplemented missing genres for existing movie title={} sourceId={}", listItem.getTitle(), listItem.getSourceId());
                    return true;
                }
                log.info("[INFO] Skip duplicate title={} sourceId={}", listItem.getTitle(), listItem.getSourceId());
                return false;
            }

            // 4. Map to Entity
            Movie movie = movieMapper.toEntity(listItem, detail, crawlerClient.getName());

            // 5. Enrich Movie (Media Upload, Genres)
            movieEnricher.enrich(movie, listItem, detail);

            // 6. Business Validate
            businessValidator.validateMovie(movie);

            // 7. Save to DB
            movieRepository.save(movie);
            
            log.info("[INFO] Movie inserted title={} sourceId={}", listItem.getTitle(), listItem.getSourceId());
            return true;

        } catch (Exception e) {
            log.error("[ERROR] Failed to process movie title={} sourceId={}. Reason: {}", listItem.getTitle(), listItem.getSourceId(), e.getMessage(), e);
            throw e; // Let the caller count it as failed
        }
    }
}
