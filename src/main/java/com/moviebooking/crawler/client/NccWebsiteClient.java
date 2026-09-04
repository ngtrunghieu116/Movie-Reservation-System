package com.moviebooking.crawler.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.crawler.config.CrawlerProperties;
import com.moviebooking.crawler.dto.MovieDetailDTO;
import com.moviebooking.crawler.dto.MovieListItemDTO;
import com.moviebooking.crawler.dto.ShowtimeItemDTO;
import com.moviebooking.crawler.exception.CrawlerException;
import com.moviebooking.crawler.exception.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NCC Website Client.
 * 
 * Architecture:
 * The NCC website (chieuphimquocgia.com.vn) is a Next.js App Router application.
 * Movie data is embedded directly in the RSC (React Server Components) payload
 * on the homepage ("/") inside <script> tags as self.__next_f.push([1, "..."]) calls.
 * 
 * Data structure found in RSC payload:
 * - "movies": [...] (Phim đang chiếu trên trang chủ)
 * - "upcomingMovies": [...] (Phim sắp chiếu trên trang chủ)
 * - "showTimes": [...] (Phim có suất chiếu trên trang /movies)
 * 
 * Each film object contains:
 * Id, FilmName, FilmNameEn, Duration, Director, Actors, Introduction,
 * ImageUrl, VideoUrl, PremieredDay, Category, CountryName, AgeAbove,
 * AgeAboveShow, StatusCode, LanguageCode, BannerUrl
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NccWebsiteClient implements CrawlerClient {

    private final RestClient restClient;
    private final CrawlerProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String NCC_NAME = "NCC";

    // Pattern to extract RSC payload from self.__next_f.push([1, "..."]) 
    private static final Pattern RSC_PUSH_PATTERN = 
            Pattern.compile("self\\.__next_f\\.push\\((\\[\\d+,\".*?\"\\])\\)");

    private static final Pattern AGE_SUFFIX_PATTERN = 
            Pattern.compile("[-_\\s]+(C18|T18|18\\+|C16|T16|16\\+|C13|T13|13\\+)\\s*$", Pattern.CASE_INSENSITIVE);

    private static final String MOVIES_KEY = "movies";
    private static final String UPCOMING_MOVIES_KEY = "upcomingMovies";
    private static final String SHOW_TIMES_KEY = "showTimes";
    
    private static final DateTimeFormatter PREMIERED_DAY_FORMAT = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Cache parsed detail data keyed by sourceId, populated during fetchMovieList
    private final Map<String, MovieDetailDTO> detailCache = new HashMap<>();

    @Override
    public String getName() {
        return NCC_NAME;
    }

    @Override
    @Retryable(
            value = {CrawlerException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public List<MovieListItemDTO> fetchMovieList() {
        log.info("[INFO] Start fetch movie list Source={} URL={}/", NCC_NAME, properties.getBaseUrl());
        String html = fetchHtml(properties.getBaseUrl() + "/");
        return parseMovieList(html);
    }

    @Override
    @Retryable(
            value = {CrawlerException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public MovieDetailDTO fetchMovieDetail(String detailUrl) {
        // NCC website embeds ALL movie data in the homepage RSC payload.
        // So detail data is already cached from fetchMovieList().
        // The detailUrl here is the sourceId (e.g., "ncc:11267")
        if (detailCache.containsKey(detailUrl)) {
            log.info("[INFO] Movie detail found in cache sourceId={}", detailUrl);
            return detailCache.get(detailUrl);
        }
        
        // If cache miss, return null - orchestrator will handle gracefully
        log.warn("[WARN] Movie detail not found in cache sourceId={}. " +
                 "NCC embeds all data in homepage.", detailUrl);
        return null;
    }

    @Override
    @Retryable(
            value = {CrawlerException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public List<ShowtimeItemDTO> fetchShowtimeList() {
        log.info("[INFO] Start fetch showtime list Source={} URL={}/movies", NCC_NAME, properties.getBaseUrl());
        String html = fetchHtml(properties.getBaseUrl() + "/movies");
        return parseShowtimeList(html);
    }

    // =========================================================================
    // HTTP
    // =========================================================================

    private String fetchHtml(String url) {
        try {
            long start = System.currentTimeMillis();
            String body = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            long elapsed = System.currentTimeMillis() - start;
            log.info("[INFO] Fetched HTML url={} time={}ms size={}", url, elapsed, 
                     body != null ? body.length() : 0);
            return body;
        } catch (Exception e) {
            log.error("[ERROR] HTTP request failed url={} reason={}", url, e.getMessage());
            throw new CrawlerException("HTTP request failed for URL: " + url, e);
        }
    }

    // =========================================================================
    // RSC Payload Extraction
    // =========================================================================

    /**
     * Extracts and concatenates all RSC payload fragments from the HTML.
     * Next.js App Router streams data via self.__next_f.push([1, "..."]) calls.
     */
    private String extractRscPayload(String html) {
        StringBuilder payload = new StringBuilder();
        Matcher matcher = RSC_PUSH_PATTERN.matcher(html);
        while (matcher.find()) {
            try {
                String jsonArrayStr = matcher.group(1);
                // Parse the array [1, "..."] using Jackson to properly unescape the string
                JsonNode node = objectMapper.readTree(jsonArrayStr);
                if (node.isArray() && node.size() > 1 && node.get(1).isTextual()) {
                    payload.append(node.get(1).asText());
                }
            } catch (Exception e) {
                log.warn("Failed to parse RSC fragment: {}", e.getMessage());
            }
        }
        return payload.toString();
    }

    /**
     * Extracts a JSON array by key from the RSC payload using bracket-matching.
     */
    private String extractArrayByKey(String rscPayload, String key) {
        String searchKey = "\"" + key + "\":[";
        int startIdx = rscPayload.indexOf(searchKey);
        if (startIdx == -1) {
            return null;
        }
        
        int arrayStart = startIdx + searchKey.length() - 1;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = arrayStart; i < rscPayload.length(); i++) {
            char c = rscPayload.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) continue;
            
            if (c == '[') depth++;
            if (c == ']') {
                depth--;
                if (depth == 0) {
                    return rscPayload.substring(arrayStart, i + 1);
                }
            }
        }
        return null;
    }

    // =========================================================================
    // Movie List Parsing
    // =========================================================================

    private List<MovieListItemDTO> parseMovieList(String html) {
        List<MovieListItemDTO> movies = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>(); // Deduplicate by NCC film ID
        
        try {
            String rscPayload = extractRscPayload(html);
            if (rscPayload.isEmpty()) {
                throw new ParseException("RSC payload is empty. Website structure may have changed.", null);
            }
            
            String moviesJson = extractArrayByKey(rscPayload, MOVIES_KEY);
            String upcomingJson = extractArrayByKey(rscPayload, UPCOMING_MOVIES_KEY);
            String showTimesJson = extractArrayByKey(rscPayload, SHOW_TIMES_KEY);

            if (moviesJson == null && upcomingJson == null && showTimesJson == null) {
                throw new ParseException("Neither 'movies', 'upcomingMovies', nor 'showTimes' data found in RSC payload.", null);
            }

            if (moviesJson != null) {
                log.info("[INFO] Extracted 'movies' JSON length={}", moviesJson.length());
                processMovieArray(moviesJson, rscPayload, movies, seenIds);
            }

            if (upcomingJson != null) {
                log.info("[INFO] Extracted 'upcomingMovies' JSON length={}", upcomingJson.length());
                processMovieArray(upcomingJson, rscPayload, movies, seenIds);
            }

            if (showTimesJson != null) {
                log.info("[INFO] Extracted 'showTimes' JSON length={}", showTimesJson.length());
                processShowTimesArray(showTimesJson, rscPayload, movies, seenIds);
            }

            log.info("[INFO] Parsed movie list total={} unique films", movies.size());
            return movies;
            
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ERROR] Failed to parse movie list reason={}", e.getMessage(), e);
            throw new ParseException("Failed to parse movie list", e);
        }
    }

    private void processMovieArray(String jsonArrayStr, String rscPayload, List<MovieListItemDTO> movies, Set<Integer> seenIds) throws Exception {
        JsonNode movieArray = objectMapper.readTree(jsonArrayStr);
        if (!movieArray.isArray()) return;

        for (JsonNode filmNode : movieArray) {
            int filmId = filmNode.path("Id").asInt(0);
            if (filmId == 0 || seenIds.contains(filmId)) continue;
            seenIds.add(filmId);
            
            MovieListItemDTO listItem = parseFilmToListItem(filmNode, filmId);
            if (listItem != null) {
                movies.add(listItem);
                
                // Cache detail data so fetchMovieDetail doesn't need another HTTP call
                MovieDetailDTO detail = parseFilmToDetail(filmNode, rscPayload);
                detailCache.put(listItem.getSourceId(), detail);
            }
        }
    }

    private void processShowTimesArray(String showTimesJsonStr, String rscPayload, List<MovieListItemDTO> movies, Set<Integer> seenIds) throws Exception {
        JsonNode showTimesArray = objectMapper.readTree(showTimesJsonStr);
        if (!showTimesArray.isArray()) return;

        for (JsonNode showTime : showTimesArray) {
            JsonNode lstFilm = showTime.get("lstFilm");
            if (lstFilm == null || !lstFilm.isArray()) continue;
            
            for (JsonNode filmNode : lstFilm) {
                int filmId = filmNode.path("Id").asInt(0);
                if (filmId == 0 || seenIds.contains(filmId)) continue;
                seenIds.add(filmId);
                
                MovieListItemDTO listItem = parseFilmToListItem(filmNode, filmId);
                if (listItem != null) {
                    movies.add(listItem);
                    MovieDetailDTO detail = parseFilmToDetail(filmNode, rscPayload);
                    detailCache.put(listItem.getSourceId(), detail);
                }
            }
        }
    }

    // =========================================================================
    // Film Node → DTO Mapping (parse* methods)
    // =========================================================================

    private MovieListItemDTO parseFilmToListItem(JsonNode filmNode, int filmId) {
        try {
            String filmName = parseTitle(filmNode);
            String filmNameEn = filmNode.path("FilmNameEn").asText("");
            String imageUrl = parsePosterUrl(filmNode);
            String bannerUrl = parseBannerUrl(filmNode);
            LocalDate premieredDay = parseReleaseDate(filmNode);
            String ageRating = parseAgeRatingRaw(filmNode);
            
            String sourceId = NCC_NAME.toLowerCase() + ":" + filmId;

            return MovieListItemDTO.builder()
                    .sourceId(sourceId)
                    .title(filmName)
                    .titleEn(filmNameEn.isBlank() ? null : filmNameEn)
                    .posterUrl(imageUrl)
                    .bannerUrl(bannerUrl)
                    .releaseDate(premieredDay)
                    .ageRatingRaw(ageRating)
                    .detailUrl(sourceId) // Used as cache key for detail lookup
                    .build();
                    
        } catch (Exception e) {
            log.warn("[WARN] Failed to parse film node Id={} reason={}", filmId, e.getMessage());
            return null;
        }
    }

    private String parseAgeRatingRaw(JsonNode filmNode) {
        // Level 1: AgeAboveShow (e.g. "C16", "C18", "C13", "P")
        String ageShow = filmNode.path("AgeAboveShow").asText("");
        if (!ageShow.isBlank()) {
            return ageShow;
        }

        // Level 2: AgeAbove integer field (e.g. 16, 18, 13)
        int ageAboveInt = filmNode.path("AgeAbove").asInt(0);
        if (ageAboveInt > 0) {
            return String.valueOf(ageAboveInt);
        }

        // Level 3: Regex match from raw FilmName suffix (e.g. "SPIDER MAN - T16")
        String rawTitle = filmNode.path("FilmName").asText("");
        Matcher matcher = AGE_SUFFIX_PATTERN.matcher(rawTitle);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    private MovieDetailDTO parseFilmToDetail(JsonNode filmNode, String rscPayload) {
        String description = parseDescription(filmNode, rscPayload);
        String director = parseDirector(filmNode);
        String actors = parseActors(filmNode);
        Integer duration = parseDuration(filmNode);
        String trailerUrl = parseTrailerUrl(filmNode);
        List<String> genres = parseGenres(filmNode);
        String language = filmNode.path("LanguageCode").asText("");

        return MovieDetailDTO.builder()
                .description(description)
                .director(director)
                .actors(actors)
                .duration(duration)
                .trailerUrl(trailerUrl)
                .genres(genres)
                .language(language)
                .build();
    }

    // =========================================================================
    // Individual Field Parsers
    // =========================================================================

    private String parseTitle(JsonNode filmNode) {
        String raw = filmNode.path("FilmName").asText("");
        if (raw.isBlank()) return "";
        // Remove age rating suffix like "-T16", "-T13", "-P", "-K" at end
        // Also remove language suffix like "(PHỤ ĐỀ)", "(LỒNG TIẾNG)"
        String cleaned = raw.replaceAll("\\s*-\\s*(T18|T16|T13|P|K|C|C18|C16|C13)\\s*$", "")
                           .replaceAll("\\s*\\(PHỤ ĐỀ\\)\\s*$", "")
                           .replaceAll("\\s*\\(LỒNG TIẾNG\\)\\s*$", "")
                           .replaceAll("\\s*\\(LT\\)\\s*$", "")
                           .replaceAll("\\s*\\(PĐ\\)\\s*$", "")
                           .trim();
        return cleaned.isEmpty() ? raw.trim() : cleaned;
    }

    private String parsePosterUrl(JsonNode filmNode) {
        return filmNode.path("ImageUrl").asText("");
    }

    private String parseBannerUrl(JsonNode filmNode) {
        String banner = filmNode.path("BannerUrl").asText("").trim();
        if (banner.isBlank() || banner.equals("null") || banner.equals(".")) {
            // Fallback to ImageUrl if BannerUrl is absent
            banner = filmNode.path("ImageUrl").asText("").trim();
        }
        return (banner.isBlank() || banner.equals("null") || banner.equals(".")) ? null : banner;
    }

    private LocalDate parseReleaseDate(JsonNode filmNode) {
        String dateStr = filmNode.path("PremieredDay").asText("");
        if (dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr, PREMIERED_DAY_FORMAT);
        } catch (DateTimeParseException e) {
            log.warn("[WARN] Failed to parse release date: {}", dateStr);
            return null;
        }
    }

    private String parseDescription(JsonNode filmNode, String rscPayload) {
        String intro = filmNode.path("Introduction").asText("");
        // NCC sometimes uses RSC references like "$1d" for long descriptions
        if (intro.startsWith("$") && intro.length() <= 5) {
            // Try to resolve the RSC reference from the payload
            String refId = intro.substring(1);
            String resolved = resolveRscReference(rscPayload, refId);
            return resolved != null ? resolved : "";
        }
        return intro;
    }

    private String parseDirector(JsonNode filmNode) {
        return filmNode.path("Director").asText("");
    }

    private String parseActors(JsonNode filmNode) {
        return filmNode.path("Actors").asText("");
    }

    private Integer parseDuration(JsonNode filmNode) {
        int duration = filmNode.path("Duration").asInt(0);
        return duration > 0 ? duration : null;
    }

    private String parseTrailerUrl(JsonNode filmNode) {
        String url = filmNode.path("VideoUrl").asText("");
        // Filter out placeholder values
        if (url.isBlank() || url.equals(".") || url.equals("null")) return "";
        return url;
    }

    private List<String> parseGenres(JsonNode filmNode) {
        String category = filmNode.path("Category").asText("");
        if (category.isBlank()) return List.of();
        // Category is comma-separated: "Hành động, Thần thoại"
        return Arrays.stream(category.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Resolves an RSC text reference (e.g., "$1d" refers to payload line "1d:T5b9,..." 
     * followed by the actual text content).
     */
    private String resolveRscReference(String rscPayload, String refId) {
        // RSC format: the referenced text appears after "refId:T<hex_length>,"
        String marker = refId + ":T";
        int idx = rscPayload.indexOf(marker);
        if (idx == -1) return null;
        
        // Skip past "refId:Txxxx,"
        int commaIdx = rscPayload.indexOf(",", idx + marker.length());
        if (commaIdx == -1) return null;
        
        // The text content follows after the comma
        int textStart = commaIdx + 1;
        // Find the end - it's terminated by the next RSC marker (pattern: "\nXX:")
        int textEnd = rscPayload.indexOf("\n", textStart);
        if (textEnd == -1) textEnd = rscPayload.length();
        
        String text = rscPayload.substring(textStart, textEnd).trim();
        return text.isEmpty() ? null : text;
    }

    // =========================================================================
    // Showtime List Parsing
    // =========================================================================

    private List<ShowtimeItemDTO> parseShowtimeList(String html) {
        List<ShowtimeItemDTO> showtimes = new ArrayList<>();
        Set<String> seenSessionIds = new HashSet<>();

        try {
            String rscPayload = extractRscPayload(html);
            if (rscPayload.isEmpty()) {
                throw new ParseException("RSC payload is empty on /movies page.", null);
            }

            String showTimesJson = extractArrayByKey(rscPayload, SHOW_TIMES_KEY);
            if (showTimesJson == null) {
                log.warn("[WARN] No 'showTimes' array found in RSC payload on /movies page.");
                return showtimes;
            }

            JsonNode showTimesArray = objectMapper.readTree(showTimesJson);
            if (!showTimesArray.isArray()) {
                return showtimes;
            }

            for (JsonNode dayNode : showTimesArray) {
                JsonNode lstFilm = dayNode.path("lstFilm");
                if (!lstFilm.isArray()) continue;

                for (JsonNode filmNode : lstFilm) {
                    int filmId = filmNode.path("Id").asInt(0);
                    if (filmId == 0) continue;
                    String filmTitle = parseTitle(filmNode);
                    String filmSourceId = NCC_NAME.toLowerCase() + ":" + filmId;

                    JsonNode lstSession = filmNode.path("lstSession");
                    if (!lstSession.isArray()) continue;

                    for (JsonNode sessionNode : lstSession) {
                        int sessionId = sessionNode.path("Id").asInt(0);
                        if (sessionId == 0) continue;

                        String sourceId = NCC_NAME.toLowerCase() + ":" + sessionId;
                        if (seenSessionIds.contains(sourceId)) continue;
                        seenSessionIds.add(sourceId);

                        ShowtimeItemDTO dto = parseSessionToDto(sessionNode, filmSourceId, filmTitle, sourceId);
                        if (dto != null) {
                            showtimes.add(dto);
                        }
                    }
                }
            }

            log.info("[INFO] Parsed showtime list total={} unique sessions from /movies", showtimes.size());
            return showtimes;
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ERROR] Failed to parse showtime list reason={}", e.getMessage(), e);
            throw new ParseException("Failed to parse showtime list", e);
        }
    }

    private ShowtimeItemDTO parseSessionToDto(JsonNode sessionNode, String filmSourceId, String filmTitle, String sourceId) {
        try {
            int roomIdInt = sessionNode.path("RoomId").asInt(0);
            if (roomIdInt == 0) return null;
            String roomSourceId = String.valueOf(roomIdInt);

            String projectTimeStr = sessionNode.path("ProjectTime").asText("");
            if (projectTimeStr.isBlank()) return null;

            LocalDateTime startTime;
            try {
                startTime = LocalDateTime.parse(projectTimeStr);
            } catch (Exception e) {
                log.warn("[WARN] Failed to parse ProjectTime string='{}': {}", projectTimeStr, e.getMessage());
                return null;
            }

            String pricePos1 = sessionNode.path("PriceOfPosition1").asText("");
            String pricePos2 = sessionNode.path("PriceOfPosition2").asText("");
            String pricePos3 = sessionNode.path("PriceOfPosition3").asText("");

            int isOnlineSellingInt = sessionNode.path("IsOnlineSelling").asInt(1);
            boolean isDeleted = sessionNode.path("Deleted").asBoolean(false);
            boolean isOnline = (isOnlineSellingInt == 1) && !isDeleted;

            return ShowtimeItemDTO.builder()
                    .sourceId(sourceId)
                    .filmSourceId(filmSourceId)
                    .filmTitle(filmTitle)
                    .roomSourceId(roomSourceId)
                    .startTime(startTime)
                    .priceStandardRaw(pricePos2) // Position 2 corresponds to Standard (T)
                    .priceVipRaw(pricePos3)      // Position 3 corresponds to VIP (V)
                    .priceCoupleRaw(pricePos1)   // Position 1 corresponds to Couple (D)
                    .isOnlineSelling(isOnline)
                    .deleted(isDeleted)
                    .build();
        } catch (Exception e) {
            log.warn("[WARN] Failed to parse session node sourceId={} reason={}", sourceId, e.getMessage());
            return null;
        }
    }
}
