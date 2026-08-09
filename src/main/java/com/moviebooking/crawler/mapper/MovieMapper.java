package com.moviebooking.crawler.mapper;

import com.moviebooking.crawler.client.CrawlerClient;
import com.moviebooking.crawler.dto.MovieDetailDTO;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import com.moviebooking.model.enums.AgeRating;
import com.moviebooking.model.Movie;
import com.moviebooking.model.enums.MovieStatus;
import com.moviebooking.service.movie.resolver.MovieStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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

    private AgeRating parseAgeRating(String rawRating) {
        if (rawRating == null) return AgeRating.P;
        String upper = rawRating.toUpperCase();
        if (upper.contains("T18") || upper.contains("18+")) return AgeRating.T18;
        if (upper.contains("T16") || upper.contains("16+")) return AgeRating.T16;
        if (upper.contains("T13") || upper.contains("13+")) return AgeRating.T13;
        return AgeRating.P; // Default
    }
}
