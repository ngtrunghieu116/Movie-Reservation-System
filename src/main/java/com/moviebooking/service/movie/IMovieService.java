package com.moviebooking.service.movie;

import com.moviebooking.dto.req.MovieRequest;
import com.moviebooking.dto.res.MovieResponse;
import com.moviebooking.model.enums.MovieStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IMovieService {
    List<MovieResponse> getAllMovies(MovieStatus status, String search);
    MovieResponse getMovieById(Long id);
    MovieResponse createMovie(MovieRequest request, MultipartFile posterFile);
    MovieResponse updateMovie(Long id, MovieRequest request, MultipartFile posterFile);
    void deleteMovie(Long id);
    List<MovieResponse> getNowShowingMovies();
    List<MovieResponse> getComingSoonMovies();
}
