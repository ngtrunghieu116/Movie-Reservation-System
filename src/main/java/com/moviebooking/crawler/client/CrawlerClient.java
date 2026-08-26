package com.moviebooking.crawler.client;

import com.moviebooking.crawler.dto.MovieDetailDTO;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import com.moviebooking.crawler.dto.ShowtimeItemDTO;

import java.util.List;

public interface CrawlerClient {
    /**
     * Gets the unique name of this crawler client (e.g. "NCC").
     */
    String getName();

    /**
     * Fetches the list of movies currently showing or coming soon.
     */
    List<MovieListItemDTO> fetchMovieList();

    /**
     * Fetches detailed information for a specific movie using its detail URL.
     */
    MovieDetailDTO fetchMovieDetail(String detailUrl);

    /**
     * Fetches the list of raw showtimes (sessions) across multiple screening dates.
     */
    List<ShowtimeItemDTO> fetchShowtimeList();
}
