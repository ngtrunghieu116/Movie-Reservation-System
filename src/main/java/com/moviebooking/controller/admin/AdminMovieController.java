package com.moviebooking.controller.admin;

import com.moviebooking.dto.req.MovieRequest;
import com.moviebooking.dto.res.MovieResponse;
import com.moviebooking.model.enums.MovieStatus;
import com.moviebooking.service.movie.IMovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
public class AdminMovieController {

    private final IMovieService movieService;

    @GetMapping
    public ResponseEntity<?> getAllMovies(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) String search) {
        if (page != null && size != null) {
            return ResponseEntity.ok(movieService.getMoviesPaged(page, size, status, search));
        }
        return ResponseEntity.ok(movieService.getAllMovies(status, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MovieResponse> createMovie(
            @Valid @RequestPart("movie") MovieRequest request,
            @RequestPart("posterFile") MultipartFile posterFile) {
        MovieResponse response = movieService.createMovie(request, posterFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable Long id,
            @Valid @RequestPart("movie") MovieRequest request,
            @RequestPart(value = "posterFile", required = false) MultipartFile posterFile) {
        MovieResponse response = movieService.updateMovie(id, request, posterFile);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.ok("Xóa bộ phim thành công!");
    }
}
