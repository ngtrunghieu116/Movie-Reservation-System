package com.moviebooking.service.genre;

import com.moviebooking.dto.req.GenreRequest;
import com.moviebooking.dto.res.GenreResponse;
import com.moviebooking.dto.res.PageResponse;

import java.util.List;

public interface IGenreService {
    List<GenreResponse> getAllGenres();
    PageResponse<GenreResponse> getGenresPaged(int pageNo, int pageSize, String search);
    GenreResponse getGenreById(Long id);
    GenreResponse createGenre(GenreRequest request);
    GenreResponse updateGenre(Long id, GenreRequest request);
    void deleteGenre(Long id);
}
