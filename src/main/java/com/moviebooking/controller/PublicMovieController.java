package com.moviebooking.controller;

import com.moviebooking.dto.res.MovieResponse;
import com.moviebooking.model.enums.MovieStatus;
import com.moviebooking.service.movie.IMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class PublicMovieController {

    private final IMovieService movieService;

    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies(
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(movieService.getAllMovies(status, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @GetMapping("/now-showing")
    public ResponseEntity<List<MovieResponse>> getNowShowingMovies() {
        return ResponseEntity.ok(movieService.getNowShowingMovies());
    }

    @GetMapping("/coming-soon")
    public ResponseEntity<List<MovieResponse>> getComingSoonMovies() {
        return ResponseEntity.ok(movieService.getComingSoonMovies());
    }
}
