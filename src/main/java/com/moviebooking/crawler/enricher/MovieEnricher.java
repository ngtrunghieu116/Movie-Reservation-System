package com.moviebooking.crawler.enricher;

import com.moviebooking.crawler.dto.MovieDetailDTO;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import com.moviebooking.crawler.mapper.MovieMapper;
import com.moviebooking.crawler.media.MediaStorage;
import com.moviebooking.crawler.media.MediaUploadResult;
import com.moviebooking.crawler.resolver.GenreResolver;
import com.moviebooking.model.Genre;
import com.moviebooking.model.Movie;
import com.moviebooking.model.enums.AgeRating;
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
    private final MovieMapper movieMapper;

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
        
        // Enrich Genres
        if (detail != null && detail.getGenres() != null && !detail.getGenres().isEmpty()) {
            String rawGenres = String.join(", ", detail.getGenres());
            Set<Genre> genres = genreResolver.resolve(rawGenres);
            movie.setGenres(genres);
        }
    }

    public boolean enrichExistingMovie(Movie movie, MovieListItemDTO listItem, MovieDetailDTO detail) {
        boolean updated = false;

        // 1. Enrich missing genres
        if ((movie.getGenres() == null || movie.getGenres().isEmpty()) 
                && detail != null && detail.getGenres() != null && !detail.getGenres().isEmpty()) {
            String rawGenres = String.join(", ", detail.getGenres());
            Set<Genre> genres = genreResolver.resolve(rawGenres);
            if (!genres.isEmpty()) {
                movie.setGenres(genres);
                updated = true;
            }
        }

        // 2. Enrich age rating if existing is P and crawled has non-P rating
        if (listItem != null && listItem.getAgeRatingRaw() != null && !listItem.getAgeRatingRaw().isBlank()) {
            AgeRating parsed = movieMapper.parseAgeRating(listItem.getAgeRatingRaw());
            if (parsed != AgeRating.P && movie.getAgeRating() == AgeRating.P) {
                movie.setAgeRating(parsed);
                updated = true;
                log.info("Updated age rating for existing movie title={} from P to {}", movie.getTitle(), parsed);
            }
        }

        return updated;
    }

    public boolean enrichMissingGenres(Movie movie, MovieDetailDTO detail) {
        return enrichExistingMovie(movie, null, detail);
    }
}
