package com.moviebooking.crawler.resolver;

import com.moviebooking.crawler.dto.MovieListItemDTO;
import com.moviebooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateMovieResolver {

    private final MovieRepository movieRepository;

    public enum Status {
        EXISTING,
        NEW
    }

    @Transactional(readOnly = true)
    public Status checkDuplicate(MovieListItemDTO listItem) {
        // 1. Check by sourceId
        if (movieRepository.existsBySourceId(listItem.getSourceId())) {
            log.info("Movie with sourceId {} already exists.", listItem.getSourceId());
            return Status.EXISTING;
        }
        
        // 2. Fallback: Check by title and release date (if applicable)
        // If the system has movies from other sources or manually inserted before the crawler,
        // we might want to check by title + release date. For now, checking by sourceId is enough 
        // to prevent crawler duplicates.
        
        return Status.NEW;
    }
}
