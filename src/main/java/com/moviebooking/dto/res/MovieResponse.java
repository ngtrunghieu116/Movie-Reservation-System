package com.moviebooking.dto.res;

import com.moviebooking.model.enums.AgeRating;
import com.moviebooking.model.enums.MovieStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class MovieResponse {
    private Long id;
    private String title;
    private String titleEn;
    private String description;
    private String director;
    private String actors;
    private Integer duration;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private String posterPath;
    private String bannerPath;
    private String trailerUrl;
    private AgeRating ageRating;
    private String language;
    private String subtitle;
    private MovieStatus status;
    private List<GenreResponse> genres;
    private Double averageRating;
}
