package com.moviebooking.crawler.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class MovieDetailDTO {
    private String description;
    private String director;
    private String actors;
    private Integer duration;
    private String trailerUrl;
    private List<String> genres;
    private String language;
}
