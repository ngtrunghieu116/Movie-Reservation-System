package com.moviebooking.crawler.client;

import com.moviebooking.crawler.config.CrawlerProperties;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NccWebsiteClientTest {

    private NccWebsiteClient client;

    @BeforeEach
    void setUp() {
        CrawlerProperties props = new CrawlerProperties();
        props.setBaseUrl("https://chieuphimquocgia.com.vn");
        
        // We pass null for RestClient since we'll mock the fetchHtml or test the internal parsing methods directly
        client = new NccWebsiteClient(null, props);
    }

    @Test
    void parseNccHtml_ShouldExtractMoviesSuccessfully() throws Exception {
        // 1. Read the sample HTML
        Path samplePath = Paths.get("src/test/resources/ncc-movies-sample.html");
        assertTrue(Files.exists(samplePath), "Sample HTML file should exist");
        String html = Files.readString(samplePath);
        assertFalse(html.isEmpty(), "Sample HTML should not be empty");

        // 2. Test parseMovieList which covers extractRscPayload, extractShowTimesJson, and JSON mapping
        List<MovieListItemDTO> movies = ReflectionTestUtils.invokeMethod(client, "parseMovieList", html);
        assertNotNull(movies, "Movies list should not be null");
        assertFalse(movies.isEmpty(), "Movies list should not be empty");

        // 5. Verify the parsed data
        MovieListItemDTO firstMovie = movies.get(0);
        assertNotNull(firstMovie.getSourceId(), "SourceId should be present");
        assertTrue(firstMovie.getSourceId().startsWith("ncc:"), "SourceId should start with ncc:");
        assertNotNull(firstMovie.getTitle(), "Title should be parsed");
        assertNotNull(firstMovie.getPosterUrl(), "Poster URL should be parsed");
        assertNotNull(firstMovie.getReleaseDate(), "Release Date should be parsed");
    }
}
