package com.moviebooking.crawler.mapper;

import com.moviebooking.crawler.dto.MovieDetailDTO;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import com.moviebooking.model.Movie;
import com.moviebooking.model.enums.AgeRating;
import com.moviebooking.model.enums.MovieStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.moviebooking.service.movie.resolver.MovieStatusResolver;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MovieMapperTest {

    private MovieMapper movieMapper;

    @BeforeEach
    void setUp() {
        movieMapper = new MovieMapper(new MovieStatusResolver());
    }

    @Test
    void toEntity_WithFullData_ShouldMapCorrectly() {
        LocalDate releaseDate = LocalDate.now().minusDays(5);
        MovieListItemDTO listDto = new MovieListItemDTO();
        listDto.setSourceId("ncc:123");
        listDto.setTitle("Spider Man");
        listDto.setReleaseDate(releaseDate);
        listDto.setAgeRatingRaw("T18");

        MovieDetailDTO detailDto = new MovieDetailDTO();
        detailDto.setDescription("Action movie");
        detailDto.setDirector("John Doe");
        detailDto.setActors("Tom, Zendaya");
        detailDto.setDuration(150);
        detailDto.setTrailerUrl("http://youtube.com");
        detailDto.setLanguage("Phụ đề Tiếng Việt");

        Movie movie = movieMapper.toEntity(listDto, detailDto, "NCC");

        assertEquals("NCC", movie.getSource());
        assertEquals("ncc:123", movie.getSourceId());
        assertEquals("Spider Man", movie.getTitle());
        assertEquals(releaseDate, movie.getReleaseDate());
        assertEquals(releaseDate.plusMonths(1), movie.getEndDate()); // Default plus 1 month
        assertEquals(AgeRating.T18, movie.getAgeRating());
        assertEquals("Action movie", movie.getDescription());
        assertEquals("John Doe", movie.getDirector());
        assertEquals("Tom, Zendaya", movie.getActors());
        assertEquals(150, movie.getDuration());
        assertEquals("http://youtube.com", movie.getTrailerUrl());
        assertEquals("Phụ đề Tiếng Việt", movie.getLanguage());
        assertEquals(MovieStatus.NOW_SHOWING, movie.getStatus());
    }

    @ParameterizedTest
    @CsvSource({
        "P, P",
        "K, P",
        "0, P",
        "C13, T13",
        "T13, T13",
        "13+, T13",
        "13, T13",
        "C-13, T13",
        "C16, T16",
        "T16, T16",
        "16+, T16",
        "16, T16",
        "C-16, T16",
        "C18, T18",
        "T18, T18",
        "18+, T18",
        "18, T18",
        "C-18, T18",
        "UNKNOWN_STRING, P"
    })
    void parseAgeRating_ShouldMapAllFormatsCorrectly(String inputRaw, AgeRating expectedRating) {
        AgeRating actual = movieMapper.parseAgeRating(inputRaw);
        assertEquals(expectedRating, actual, "Failed for raw string: " + inputRaw);
    }

    @Test
    void toEntity_WithMissingEndDate_ShouldDefaultToOneMonthAfterRelease() {
        MovieListItemDTO listDto = new MovieListItemDTO();
        listDto.setReleaseDate(LocalDate.of(2023, 5, 10));
        
        Movie movie = movieMapper.toEntity(listDto, new MovieDetailDTO(), "NCC");
        
        assertEquals(LocalDate.of(2023, 6, 10), movie.getEndDate());
    }

    @Test
    void toEntity_WithNullDuration_ShouldDefaultTo120() {
        MovieListItemDTO listDto = new MovieListItemDTO();
        MovieDetailDTO detailDto = new MovieDetailDTO();
        detailDto.setDuration(null);
        
        Movie movie = movieMapper.toEntity(listDto, detailDto, "NCC");
        
        assertEquals(120, movie.getDuration());
    }

    @Test
    void toEntity_WithNullReleaseDate_ShouldDefaultEndDateAndStatus() {
        MovieListItemDTO listDto = new MovieListItemDTO();
        listDto.setReleaseDate(null);
        
        Movie movie = movieMapper.toEntity(listDto, new MovieDetailDTO(), "NCC");
        
        assertNull(movie.getReleaseDate());
        assertNotNull(movie.getEndDate());
        assertEquals(LocalDate.now().plusMonths(1), movie.getEndDate());
        assertEquals(MovieStatus.COMING_SOON, movie.getStatus());
    }
}
