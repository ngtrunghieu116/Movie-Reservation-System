package com.moviebooking.crawler.enricher;

import com.moviebooking.crawler.dto.MovieDetailDTO;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import com.moviebooking.crawler.media.MediaStorage;
import com.moviebooking.crawler.media.MediaUploadResult;
import com.moviebooking.crawler.resolver.GenreResolver;
import com.moviebooking.model.Genre;
import com.moviebooking.model.Movie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieEnricher {

    private final MediaStorage mediaStorage;
    private final GenreResolver genreResolver;

    public void enrich(Movie movie, MovieListItemDTO listItem, MovieDetailDTO detail) {
        log.info("Enriching movie: {}", movie.getTitle());
        
        // Enrich Media (Poster)
        if (listItem.getPosterUrl() != null && !listItem.getPosterUrl().isBlank()) {
            MediaUploadResult posterResult = mediaStorage.uploadFromUrl(listItem.getPosterUrl());
            if (posterResult.success()) {
                movie.setPosterPath(posterResult.url());
            } else {
                log.warn("Failed to upload poster for movie: {}. Fallback to original URL.", movie.getTitle());
                movie.setPosterPath(listItem.getPosterUrl());
            }
        }
        
        // Enrich Media (Banner) - if detail has it or logic requires it
        // For now we don't have banner URL from crawler DTO, so we skip it.
        
        // Enrich Genres
        if (detail != null && detail.getGenres() != null && !detail.getGenres().isEmpty()) {
            // Convert List of String genres to a comma-separated string for resolver
            String rawGenres = String.join(", ", detail.getGenres());
            Set<Genre> genres = genreResolver.resolve(rawGenres);
            movie.setGenres(genres);
        }
    }

    public boolean enrichMissingGenres(Movie movie, MovieDetailDTO detail) {
        if ((movie.getGenres() == null || movie.getGenres().isEmpty()) 
                && detail != null && detail.getGenres() != null && !detail.getGenres().isEmpty()) {
            String rawGenres = String.join(", ", detail.getGenres());
            Set<Genre> genres = genreResolver.resolve(rawGenres);
            if (!genres.isEmpty()) {
                movie.setGenres(genres);
                return true;
            }
        }
        return false;
    }
}
