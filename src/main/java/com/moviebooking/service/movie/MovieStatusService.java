package com.moviebooking.service.movie;

import com.moviebooking.model.Movie;
import com.moviebooking.model.enums.MovieStatus;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.service.movie.resolver.MovieStatusResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieStatusService {

    private final MovieRepository movieRepository;
    private final MovieStatusResolver statusResolver;

    /**
     * Updates status for all active movies (COMING_SOON and NOW_SHOWING).
     * Only performs DB updates when the calculated status differs from the current status.
     */
    @Transactional
    public int updateAllStatuses() {
        log.info("[INFO] Starting daily movie status update...");
        LocalDate today = LocalDate.now();

        List<Movie> activeMovies = movieRepository.findByStatusIn(
                List.of(MovieStatus.COMING_SOON, MovieStatus.NOW_SHOWING)
        );

        int updatedCount = 0;

        for (Movie movie : activeMovies) {
            MovieStatus currentStatus = movie.getStatus();
            MovieStatus newStatus = statusResolver.resolveStatus(movie.getReleaseDate(), movie.getEndDate(), today);

            if (newStatus != currentStatus) {
                movie.setStatus(newStatus);
                movieRepository.save(movie);
                updatedCount++;
                log.info("[INFO] Updated movie status id={} title='{}' from {} to {}",
                        movie.getId(), movie.getTitle(), currentStatus, newStatus);
            }
        }

        log.info("[INFO] Completed movie status update. Total active movies checked={}, total updated={}",
                activeMovies.size(), updatedCount);

        return updatedCount;
    }
}
