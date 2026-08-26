package com.moviebooking.crawler.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moviebooking.crawler.config.CrawlerProperties;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import com.moviebooking.crawler.dto.ShowtimeItemDTO;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        CrawlerProperties props = new CrawlerProperties();
        props.setBaseUrl("https://chieuphimquocgia.com.vn");
        
        // We pass null for RestClient since we'll test the internal parsing methods directly
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

        // 3. Verify the parsed data
        MovieListItemDTO firstMovie = movies.get(0);
        assertNotNull(firstMovie.getSourceId(), "SourceId should be present");
        assertTrue(firstMovie.getSourceId().startsWith("ncc:"), "SourceId should start with ncc:");
        assertNotNull(firstMovie.getTitle(), "Title should be parsed");
        assertNotNull(firstMovie.getPosterUrl(), "Poster URL should be parsed");
        assertNotNull(firstMovie.getReleaseDate(), "Release Date should be parsed");
    }

    @Test
    void parseNccHtml_ShouldExtractShowtimesSuccessfully() throws Exception {
        // 1. Read the sample HTML containing live NCC RSC payload
        Path samplePath = Paths.get("src/test/resources/ncc-movies-sample.html");
        assertTrue(Files.exists(samplePath), "Sample HTML file should exist");
        String html = Files.readString(samplePath);
        assertFalse(html.isEmpty(), "Sample HTML should not be empty");

        // 2. Test parseShowtimeList
        List<ShowtimeItemDTO> showtimes = ReflectionTestUtils.invokeMethod(client, "parseShowtimeList", html);
        assertNotNull(showtimes, "Showtimes list should not be null");
        assertFalse(showtimes.isEmpty(), "Showtimes list should not be empty");

        // 3. Verify properties of parsed showtime DTOs
        ShowtimeItemDTO firstShowtime = showtimes.get(0);
        assertNotNull(firstShowtime.getSourceId(), "SourceId must be parsed");
        assertTrue(firstShowtime.getSourceId().startsWith("ncc:"), "SourceId must start with ncc:");
        assertNotNull(firstShowtime.getFilmSourceId(), "FilmSourceId must be parsed");
        assertTrue(firstShowtime.getFilmSourceId().startsWith("ncc:"), "FilmSourceId must start with ncc:");
        assertNotNull(firstShowtime.getRoomSourceId(), "RoomSourceId must be present");
        assertFalse(firstShowtime.getRoomSourceId().isBlank(), "RoomSourceId must not be blank");
        assertNotNull(firstShowtime.getStartTime(), "StartTime must be parsed");
        assertNotNull(firstShowtime.getIsOnlineSelling(), "IsOnlineSelling flag must be present");
        assertNotNull(firstShowtime.getDeleted(), "Deleted flag must be present");

        // Verify prices format
        assertNotNull(firstShowtime.getPriceStandardRaw(), "PriceStandardRaw should not be null");
        assertNotNull(firstShowtime.getPriceVipRaw(), "PriceVipRaw should not be null");
        assertNotNull(firstShowtime.getPriceCoupleRaw(), "PriceCoupleRaw should not be null");
    }

    @Test
    void parseSessionToDto_WithMalformedSessionData_ShouldSkipSafelyWithoutException() {
        // 1. Malformed session missing RoomId (0)
        ObjectNode nodeMissingRoom = objectMapper.createObjectNode();
        nodeMissingRoom.put("Id", 12345);
        nodeMissingRoom.put("RoomId", 0);
        nodeMissingRoom.put("ProjectTime", "2026-08-22T19:00:00");

        ShowtimeItemDTO result1 = ReflectionTestUtils.invokeMethod(
                client, "parseSessionToDto", nodeMissingRoom, "ncc:100", "Sample Movie", "ncc:12345");
        assertNull(result1, "Should return null when RoomId is 0 or missing");

        // 2. Malformed session with invalid datetime
        ObjectNode nodeInvalidDate = objectMapper.createObjectNode();
        nodeInvalidDate.put("Id", 12346);
        nodeInvalidDate.put("RoomId", 2100);
        nodeInvalidDate.put("ProjectTime", "INVALID_DATE_TIME");

        ShowtimeItemDTO result2 = ReflectionTestUtils.invokeMethod(
                client, "parseSessionToDto", nodeInvalidDate, "ncc:100", "Sample Movie", "ncc:12346");
        assertNull(result2, "Should return null when ProjectTime is unparseable");

        // 3. Valid session with offline / deleted flag
        ObjectNode nodeDeleted = objectMapper.createObjectNode();
        nodeDeleted.put("Id", 12347);
        nodeDeleted.put("RoomId", 2100);
        nodeDeleted.put("ProjectTime", "2026-08-22T19:00:00");
        nodeDeleted.put("IsOnlineSelling", 1);
        nodeDeleted.put("Deleted", true);

        ShowtimeItemDTO result3 = ReflectionTestUtils.invokeMethod(
                client, "parseSessionToDto", nodeDeleted, "ncc:100", "Sample Movie", "ncc:12347");
        assertNotNull(result3, "Should parse valid structure");
        assertFalse(result3.getIsOnlineSelling(), "isOnlineSelling must be false when Deleted is true");
        assertTrue(result3.getDeleted(), "deleted must be true");
    }
}
