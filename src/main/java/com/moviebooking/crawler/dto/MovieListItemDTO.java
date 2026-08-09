package com.moviebooking.crawler.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@ToString
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class MovieListItemDTO {
    private String sourceId;
    private String title;
    private String titleEn;
    private String posterUrl;
    private LocalDate releaseDate;
    private String ageRatingRaw;
    
    // To navigate to detail page
    private String detailUrl;
}
