package com.moviebooking.crawler.validator;

import com.moviebooking.crawler.dto.MovieDetailDTO;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import com.moviebooking.crawler.exception.CrawlerException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DtoValidator {

    public void validateListItem(MovieListItemDTO item) {
        if (!StringUtils.hasText(item.getTitle())) {
            throw new CrawlerException("Title is missing or empty");
        }
        if (!StringUtils.hasText(item.getSourceId())) {
            throw new CrawlerException("Source ID is missing or empty for movie: " + item.getTitle());
        }
        if (item.getReleaseDate() == null) {
            throw new CrawlerException("Release date is missing for movie: " + item.getTitle());
        }
    }

    public void validateDetail(MovieDetailDTO detail, String title) {
        if (detail.getDuration() == null || detail.getDuration() <= 0) {
            throw new CrawlerException("Invalid duration for movie: " + title);
        }
    }
}
