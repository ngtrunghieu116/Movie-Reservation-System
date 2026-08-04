package com.moviebooking.service.genre;

import com.moviebooking.dto.req.GenreRequest;
import com.moviebooking.dto.res.GenreResponse;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.model.Genre;
import com.moviebooking.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenreService implements IGenreService {

    private final GenreRepository genreRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GenreResponse> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GenreResponse> getGenresPaged(int pageNo, int pageSize, String search) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        Page<Genre> genrePage;

        if (search != null && !search.trim().isEmpty()) {
            genrePage = genreRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        } else {
            genrePage = genreRepository.findAll(pageable);
        }

        List<GenreResponse> content = genrePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<GenreResponse>builder()
                .content(content)
                .pageNo(genrePage.getNumber())
                .pageSize(genrePage.getSize())
                .totalElements(genrePage.getTotalElements())
                .totalPages(genrePage.getTotalPages())
                .last(genrePage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
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
