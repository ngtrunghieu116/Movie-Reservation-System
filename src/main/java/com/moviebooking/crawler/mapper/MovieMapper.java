package com.moviebooking.crawler.mapper;

import com.moviebooking.crawler.dto.MovieDetailDTO;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import com.moviebooking.model.enums.AgeRating;
import com.moviebooking.model.Movie;
import com.moviebooking.service.movie.resolver.MovieStatusResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieMapper {

    private final MovieStatusResolver statusResolver;

    public Movie toEntity(MovieListItemDTO listItem, MovieDetailDTO detail, String sourceName) {
        Movie movie = new Movie();
        
        // From list item
        movie.setSource(sourceName);
        movie.setSourceId(listItem.getSourceId());
        movie.setTitle(listItem.getTitle());
        
        LocalDate releaseDate = listItem.getReleaseDate();
        LocalDate endDate = releaseDate != null ? releaseDate.plusMonths(1) : LocalDate.now().plusMonths(1);
        
        movie.setReleaseDate(releaseDate);
        movie.setEndDate(endDate);
        movie.setAgeRating(parseAgeRating(listItem.getAgeRatingRaw()));
        movie.setStatus(statusResolver.resolveStatus(releaseDate, endDate, LocalDate.now()));
        
        // Banner path and Poster path will be populated by MediaStorage later
        // Genres will be mapped by GenreResolver later

        // From detail
        if (detail != null) {
            movie.setDescription(detail.getDescription());
            movie.setDirector(detail.getDirector());
            movie.setActors(detail.getActors());
            movie.setDuration(detail.getDuration() != null ? detail.getDuration() : 120); // Default to 120 if null
            movie.setTrailerUrl(detail.getTrailerUrl());
            movie.setLanguage(detail.getLanguage() != null ? detail.getLanguage() : "Phụ đề Tiếng Việt");
            movie.setSubtitle("Phụ đề Tiếng Việt"); // Default mapping
        }

        return movie;
    }

    public AgeRating parseAgeRating(String rawRating) {
        if (rawRating == null || rawRating.isBlank()) {
            return AgeRating.P;
        }

        String cleaned = rawRating.toUpperCase().replaceAll("[\\s\\-_]", "");

        if (cleaned.contains("18") || cleaned.contains("T18") || cleaned.contains("C18")) {
            return AgeRating.T18;
        }
        if (cleaned.contains("16") || cleaned.contains("T16") || cleaned.contains("C16")) {
            return AgeRating.T16;
        }
        if (cleaned.contains("13") || cleaned.contains("T13") || cleaned.contains("C13")) {
            return AgeRating.T13;
        }
        if (cleaned.equals("P") || cleaned.equals("K") || cleaned.equals("0")) {
            return AgeRating.P;
        }

        log.warn("[MovieMapper] Unrecognized raw age rating string '{}' (cleaned='{}'). Falling back to AgeRating.P", rawRating, cleaned);
        return AgeRating.P;
    }
}
