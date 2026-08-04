package com.moviebooking.service.movie;

import com.moviebooking.dto.req.MovieRequest;
import com.moviebooking.dto.res.GenreResponse;
import com.moviebooking.dto.res.MovieResponse;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.model.Genre;
import com.moviebooking.model.Movie;
import com.moviebooking.model.enums.MovieStatus;
import com.moviebooking.repository.GenreRepository;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.service.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService implements IMovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> getAllMovies(MovieStatus status, String search) {
        List<Movie> movies;
        if (status != null && search != null && !search.trim().isEmpty()) {
            movies = movieRepository.findByStatusAndTitleContainingIgnoreCase(status, search.trim());
        } else if (status != null) {
            movies = movieRepository.findByStatus(status);
        } else if (search != null && !search.trim().isEmpty()) {
            movies = movieRepository.findByTitleContainingIgnoreCase(search.trim());
        } else {
            movies = movieRepository.findAll();
        }

        return movies.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MovieResponse> getMoviesPaged(int pageNo, int pageSize, MovieStatus status, String search) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageNo, pageSize, org.springframework.data.domain.Sort.by("id").descending()
        );
        org.springframework.data.domain.Page<Movie> moviePage;

        if (status != null && search != null && !search.trim().isEmpty()) {
            moviePage = movieRepository.findByStatusAndTitleContainingIgnoreCase(status, search.trim(), pageable);
        } else if (status != null) {
            moviePage = movieRepository.findByStatus(status, pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            moviePage = movieRepository.findByTitleContainingIgnoreCase(search.trim(), pageable);
        } else {
            moviePage = movieRepository.findAll(pageable);
        }

        List<MovieResponse> content = moviePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<MovieResponse>builder()
                .content(content)
                .pageNo(moviePage.getNumber())
                .pageSize(moviePage.getSize())
                .totalElements(moviePage.getTotalElements())
                .totalPages(moviePage.getTotalPages())
                .last(moviePage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ phim với ID: " + id));
        return mapToResponse(movie);
    }

    @Override
    @Transactional
    public MovieResponse createMovie(MovieRequest request, MultipartFile posterFile) {
        // Validation: Date logic
        if (request.getReleaseDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Ngày khởi chiếu không được diễn ra sau ngày kết thúc!");
        }

        if (movieRepository.existsByTitle(request.getTitle())) {
            throw new RuntimeException("Tên phim này đã tồn tại trong hệ thống!");
        }

        if (posterFile == null || posterFile.isEmpty()) {
            throw new RuntimeException("File ảnh poster là bắt buộc khi tạo mới phim!");
        }

        // Store File
        String posterPath = fileStorageService.storePosterFile(posterFile);

        // Map genres
        Set<Genre> genres = fetchGenresByIds(request.getGenreIds());

        Movie movie = Movie.builder()
                .title(request.getTitle())
                .titleEn(request.getTitleEn())
                .description(request.getDescription())
                .director(request.getDirector())
                .actors(request.getActors())
                .duration(request.getDuration())
                .releaseDate(request.getReleaseDate())
                .endDate(request.getEndDate())
                .posterPath(posterPath)
                .trailerUrl(request.getTrailerUrl())
                .ageRating(request.getAgeRating())
                .language(request.getLanguage())
                .subtitle(request.getSubtitle())
                .status(request.getStatus())
                .genres(genres)
                .build();

        Movie savedMovie = movieRepository.save(movie);
        return mapToResponse(savedMovie);
    }

    @Override
    @Transactional
    public MovieResponse updateMovie(Long id, MovieRequest request, MultipartFile posterFile) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ phim với ID: " + id));

        // Validation: Date logic
        if (request.getReleaseDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Ngày khởi chiếu không được diễn ra sau ngày kết thúc!");
        }

        if (movieRepository.existsByTitleAndIdNot(request.getTitle(), id)) {
            throw new RuntimeException("Tên phim này đã trùng với một bộ phim khác!");
        }

        // Handle poster file update if a new file is uploaded
        if (posterFile != null && !posterFile.isEmpty()) {
            String posterPath = fileStorageService.storePosterFile(posterFile);
            movie.setPosterPath(posterPath);
        }

        // Map genres
        Set<Genre> genres = fetchGenresByIds(request.getGenreIds());

        movie.setTitle(request.getTitle());
        movie.setTitleEn(request.getTitleEn());
        movie.setDescription(request.getDescription());
        movie.setDirector(request.getDirector());
        movie.setActors(request.getActors());
        movie.setDuration(request.getDuration());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setEndDate(request.getEndDate());
        movie.setTrailerUrl(request.getTrailerUrl());
        movie.setAgeRating(request.getAgeRating());
        movie.setLanguage(request.getLanguage());
        movie.setSubtitle(request.getSubtitle());
        movie.setStatus(request.getStatus());
        movie.setGenres(genres);

        Movie updatedMovie = movieRepository.save(movie);
        return mapToResponse(updatedMovie);
    }

    @Override
    @Transactional
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ phim với ID: " + id));
        movieRepository.delete(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> getNowShowingMovies() {
        return movieRepository.findByStatus(MovieStatus.NOW_SHOWING)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> getComingSoonMovies() {
        return movieRepository.findByStatus(MovieStatus.COMING_SOON)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private Set<Genre> fetchGenresByIds(Set<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Genre> foundGenres = genreRepository.findAllById(genreIds);
        if (foundGenres.size() != genreIds.size()) {
            throw new RuntimeException("Một hoặc nhiều ID thể loại không tồn tại!");
        }
        return new HashSet<>(foundGenres);
    }

    private MovieResponse mapToResponse(Movie movie) {
        List<GenreResponse> genreResponses = movie.getGenres().stream()
                .map(g -> GenreResponse.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .description(g.getDescription())
                        .build())
                .collect(Collectors.toList());

        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .titleEn(movie.getTitleEn())
                .description(movie.getDescription())
                .director(movie.getDirector())
                .actors(movie.getActors())
                .duration(movie.getDuration())
                .releaseDate(movie.getReleaseDate())
                .endDate(movie.getEndDate())
                .posterPath(movie.getPosterPath())
                .trailerUrl(movie.getTrailerUrl())
                .ageRating(movie.getAgeRating())
                .language(movie.getLanguage())
                .subtitle(movie.getSubtitle())
                .status(movie.getStatus())
                .genres(genreResponses)
                .averageRating(5.0) // Default rating placeholder until Review module
                .build();
    }
}
