package com.moviebooking.crawler.validator;

import com.moviebooking.model.Movie;
import com.moviebooking.crawler.exception.CrawlerException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class BusinessValidator {

    public void validateMovie(Movie movie) {
        if (movie.getReleaseDate() != null && movie.getEndDate() != null) {
            if (movie.getReleaseDate().isAfter(movie.getEndDate())) {
                throw new CrawlerException("Release date cannot be after end date for movie: " + movie.getTitle());
            }
        }
        if (movie.getReleaseDate() != null && movie.getReleaseDate().isBefore(LocalDate.of(1900, 1, 1))) {
            throw new CrawlerException("Release date is unreasonably old for movie: " + movie.getTitle());
        }
    }
}
