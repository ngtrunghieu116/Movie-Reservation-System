package com.moviebooking.service.genre;

import com.moviebooking.dto.req.GenreRequest;
import com.moviebooking.dto.res.GenreResponse;
import com.moviebooking.model.Genre;
import com.moviebooking.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenreService implements IGenreService {

    private final GenreRepository genreRepository;

    @Override
    public List<GenreResponse> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public GenreResponse getGenreById(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại phim với ID: " + id));
        return mapToResponse(genre);
    }

    @Override
    @Transactional
    public GenreResponse createGenre(GenreRequest request) {
        if (genreRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên thể loại này đã tồn tại!");
        }

        Genre genre = Genre.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Genre savedGenre = genreRepository.save(genre);
        return mapToResponse(savedGenre);
    }

    @Override
    @Transactional
    public GenreResponse updateGenre(Long id, GenreRequest request) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại phim với ID: " + id));

        if (genreRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new RuntimeException("Tên thể loại này đã tồn tại!");
        }

        genre.setName(request.getName());
        genre.setDescription(request.getDescription());

        Genre updatedGenre = genreRepository.save(genre);
        return mapToResponse(updatedGenre);
    }

    @Override
    @Transactional
    public void deleteGenre(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại phim với ID: " + id));

        if (genre.getMovies() != null && !genre.getMovies().isEmpty()) {
            throw new RuntimeException("Không thể xóa thể loại đã được gắn cho các bộ phim!");
        }

        genreRepository.delete(genre);
    }

    private GenreResponse mapToResponse(Genre genre) {
        return GenreResponse.builder()
                .id(genre.getId())
                .name(genre.getName())
                .description(genre.getDescription())
                .build();
    }
}
