package com.moviebooking.controller.admin;

import com.moviebooking.dto.req.GenreRequest;
import com.moviebooking.dto.res.GenreResponse;
import com.moviebooking.service.genre.IGenreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/genres")
@RequiredArgsConstructor
public class AdminGenreController {

    private final IGenreService genreService;

    @GetMapping
    public ResponseEntity<?> getAllGenres(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search) {
        if (page != null && size != null) {
            return ResponseEntity.ok(genreService.getGenresPaged(page, size, search));
        }
        return ResponseEntity.ok(genreService.getAllGenres());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenreResponse> getGenreById(@PathVariable Long id) {
        return ResponseEntity.ok(genreService.getGenreById(id));
    }

    @PostMapping
    public ResponseEntity<GenreResponse> createGenre(@Valid @RequestBody GenreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(genreService.createGenre(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenreResponse> updateGenre(@PathVariable Long id, @Valid @RequestBody GenreRequest request) {
        return ResponseEntity.ok(genreService.updateGenre(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteGenre(@PathVariable Long id) {
        genreService.deleteGenre(id);
        return ResponseEntity.ok(Map.of("message", "Xóa thể loại phim thành công!"));
    }
}
