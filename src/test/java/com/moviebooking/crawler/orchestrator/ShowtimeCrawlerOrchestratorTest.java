package com.moviebooking.crawler.orchestrator;

import com.moviebooking.crawler.client.CrawlerClient;
import com.moviebooking.crawler.dto.ShowtimeCrawlerSummaryResponse;
import com.moviebooking.crawler.dto.ShowtimeItemDTO;
import com.moviebooking.crawler.resolver.RoomResolver;
import com.moviebooking.crawler.resolver.ShowtimePriceParser;
import com.moviebooking.model.Movie;
import com.moviebooking.model.Room;
import com.moviebooking.model.Showtime;
import com.moviebooking.model.Theater;
import com.moviebooking.model.enums.MovieStatus;
import com.moviebooking.model.enums.RoomType;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ReservationRepository;
import com.moviebooking.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeCrawlerOrchestratorTest {

    @Mock
    private CrawlerClient crawlerClient;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomResolver roomResolver;

    @Spy
    private ShowtimePriceParser priceParser = new ShowtimePriceParser();

    @InjectMocks
    private ShowtimeCrawlerOrchestrator orchestrator;

    private Movie validNowShowingMovie;
    private Room validRoom;
    private LocalDateTime futureTime;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orchestrator, "bufferTimeMinutes", 15);
        ReflectionTestUtils.setField(orchestrator, "defaultPriceStandard", new BigDecimal("90000"));
        ReflectionTestUtils.setField(orchestrator, "defaultPriceVip", new BigDecimal("95000"));
        ReflectionTestUtils.setField(orchestrator, "defaultPriceCouple", new BigDecimal("100000"));

        futureTime = LocalDateTime.now().plusDays(1).withHour(19).withMinute(0).withSecond(0).withNano(0);

        validNowShowingMovie = Movie.builder()
                .id(100L)
                .title("Doraemon: Nobita va Ban Nhac")
                .sourceId("ncc:11059")
                .status(MovieStatus.NOW_SHOWING)
                .releaseDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(30))
                .duration(110)
                .build();

        Theater theater = Theater.builder().id(1L).name("CineMind Center").build();
        validRoom = Room.builder()
                .id(10L)
                .name("Phòng chiếu 1")
                .roomType(RoomType.TWO_D)
                .theater(theater)
                .sourceRoomId("2114")
                .isActive(true)
                .build();
    }

    private ShowtimeItemDTO buildSampleItem(String sourceId, LocalDateTime startTime) {
        return ShowtimeItemDTO.builder()
                .sourceId(sourceId)
                .filmSourceId("ncc:11059")
                .filmTitle("Doraemon: Nobita va Ban Nhac")
                .roomSourceId("2114")
                .startTime(startTime)
                .priceStandardRaw("T:90000")
                .priceVipRaw("V:95000")
                .priceCoupleRaw("D:100000")
                .isOnlineSelling(true)
                .deleted(false)
                .build();
    }

    @Test
    void crawlShowtimes_NewValidShowtime_ShouldInsertSuccessfully() {
        ShowtimeItemDTO item = buildSampleItem("ncc:412427", futureTime);
        when(crawlerClient.fetchShowtimeList()).thenReturn(List.of(item));
        when(crawlerClient.getName()).thenReturn("NCC");
        when(movieRepository.findBySourceId("ncc:11059")).thenReturn(Optional.of(validNowShowingMovie));
        when(roomResolver.resolve("2114", 1L)).thenReturn(new RoomResolver.RoomResolveResult(RoomResolver.Status.RESOLVED, validRoom));
        when(showtimeRepository.findBySourceId("ncc:412427")).thenReturn(Optional.empty());
        when(showtimeRepository.existsOverlappingShowtime(eq(10L), any(), any(), isNull())).thenReturn(false);
        when(showtimeRepository.findBySourceIdStartingWithAndIsActiveTrue("ncc:")).thenReturn(Collections.emptyList());

        ShowtimeCrawlerSummaryResponse summary = orchestrator.crawlShowtimes(1L);

        assertEquals(1, summary.getTotalFetched());
        assertEquals(1, summary.getInserted());
        assertEquals(0, summary.getUpdated());
        assertEquals(0, summary.getSkipped());
        assertEquals(0, summary.getFailed());
        verify(showtimeRepository, times(1)).save(any(Showtime.class));
    }

    @Test
    void crawlShowtimes_ExistingShowtime_ShouldUpdateLastSeenAndOnlineSelling() {
        ShowtimeItemDTO item = buildSampleItem("ncc:412427", futureTime);
        item.setIsOnlineSelling(false);

        Showtime existingShowtime = Showtime.builder()
                .id(50L)
                .movie(validNowShowingMovie)
                .room(validRoom)
                .startTime(futureTime)
                .endTime(futureTime.plusMinutes(110))
                .sourceId("ncc:412427")
                .isActive(true)
                .isOnlineSelling(true)
                .missingCount(1)
                .build();

        when(crawlerClient.fetchShowtimeList()).thenReturn(List.of(item));
        when(crawlerClient.getName()).thenReturn("NCC");
        when(movieRepository.findBySourceId("ncc:11059")).thenReturn(Optional.of(validNowShowingMovie));
        when(roomResolver.resolve("2114", 1L)).thenReturn(new RoomResolver.RoomResolveResult(RoomResolver.Status.RESOLVED, validRoom));
        when(showtimeRepository.findBySourceId("ncc:412427")).thenReturn(Optional.of(existingShowtime));
        when(reservationRepository.existsByShowtimeId(50L)).thenReturn(false);
        when(showtimeRepository.findBySourceIdStartingWithAndIsActiveTrue("ncc:")).thenReturn(List.of(existingShowtime));

        ShowtimeCrawlerSummaryResponse summary = orchestrator.crawlShowtimes(1L);

        assertEquals(1, summary.getTotalFetched());
        assertEquals(0, summary.getInserted());
        assertEquals(1, summary.getUpdated());
        assertEquals(0, summary.getSkipped());
        assertEquals(0, existingShowtime.getMissingCount());
        assertFalse(existingShowtime.getIsOnlineSelling());
        verify(showtimeRepository, times(1)).save(existingShowtime);
    }

    @Test
    void crawlShowtimes_UnmappedMovie_ShouldSkipWithLog() {
        ShowtimeItemDTO item = buildSampleItem("ncc:412427", futureTime);
        when(crawlerClient.fetchShowtimeList()).thenReturn(List.of(item));
        when(crawlerClient.getName()).thenReturn("NCC");
        when(movieRepository.findBySourceId("ncc:11059")).thenReturn(Optional.empty());
        when(showtimeRepository.findBySourceIdStartingWithAndIsActiveTrue("ncc:")).thenReturn(Collections.emptyList());

        ShowtimeCrawlerSummaryResponse summary = orchestrator.crawlShowtimes(1L);

        assertEquals(1, summary.getTotalFetched());
        assertEquals(0, summary.getInserted());
        assertEquals(1, summary.getSkipped());
        verify(showtimeRepository, never()).save(any());
    }

    @Test
    void crawlShowtimes_MovieComingSoonOrEnded_ShouldSkipWithLog() {
        validNowShowingMovie.setStatus(MovieStatus.COMING_SOON);

        ShowtimeItemDTO item = buildSampleItem("ncc:412427", futureTime);
        when(crawlerClient.fetchShowtimeList()).thenReturn(List.of(item));
        when(crawlerClient.getName()).thenReturn("NCC");
        when(movieRepository.findBySourceId("ncc:11059")).thenReturn(Optional.of(validNowShowingMovie));
        when(showtimeRepository.findBySourceIdStartingWithAndIsActiveTrue("ncc:")).thenReturn(Collections.emptyList());

        ShowtimeCrawlerSummaryResponse summary = orchestrator.crawlShowtimes(1L);

        assertEquals(1, summary.getTotalFetched());
        assertEquals(0, summary.getInserted());
        assertEquals(1, summary.getSkipped());
        verify(showtimeRepository, never()).save(any());
    }

    @Test
    void crawlShowtimes_StartTimeBeforeReleaseDate_ShouldSkipWithLog() {
        // Movie release date is in future compared to showtime
        validNowShowingMovie.setReleaseDate(futureTime.toLocalDate().plusDays(2));

        ShowtimeItemDTO item = buildSampleItem("ncc:412427", futureTime);
        when(crawlerClient.fetchShowtimeList()).thenReturn(List.of(item));
        when(crawlerClient.getName()).thenReturn("NCC");
        when(movieRepository.findBySourceId("ncc:11059")).thenReturn(Optional.of(validNowShowingMovie));
        when(showtimeRepository.findBySourceIdStartingWithAndIsActiveTrue("ncc:")).thenReturn(Collections.emptyList());

        ShowtimeCrawlerSummaryResponse summary = orchestrator.crawlShowtimes(1L);

        assertEquals(1, summary.getTotalFetched());
        assertEquals(0, summary.getInserted());
        assertEquals(1, summary.getSkipped());
        verify(showtimeRepository, never()).save(any());
    }

    @Test
    void crawlShowtimes_UnmappedOrMaintenanceRoom_ShouldSkipWithLog() {
        ShowtimeItemDTO item = buildSampleItem("ncc:412427", futureTime);
        when(crawlerClient.fetchShowtimeList()).thenReturn(List.of(item));
        when(crawlerClient.getName()).thenReturn("NCC");
        when(movieRepository.findBySourceId("ncc:11059")).thenReturn(Optional.of(validNowShowingMovie));
        when(roomResolver.resolve("2114", 1L)).thenReturn(new RoomResolver.RoomResolveResult(RoomResolver.Status.UNMAPPED_ROOM, null));
        when(showtimeRepository.findBySourceIdStartingWithAndIsActiveTrue("ncc:")).thenReturn(Collections.emptyList());

        ShowtimeCrawlerSummaryResponse summary = orchestrator.crawlShowtimes(1L);

        assertEquals(1, summary.getTotalFetched());
        assertEquals(0, summary.getInserted());
        assertEquals(1, summary.getSkipped());
        verify(showtimeRepository, never()).save(any());
    }

    @Test
    void crawlShowtimes_RoomOverlapConflict_ShouldSkipWithLog() {
        ShowtimeItemDTO item = buildSampleItem("ncc:412427", futureTime);
        when(crawlerClient.fetchShowtimeList()).thenReturn(List.of(item));
        when(crawlerClient.getName()).thenReturn("NCC");
        when(movieRepository.findBySourceId("ncc:11059")).thenReturn(Optional.of(validNowShowingMovie));
        when(roomResolver.resolve("2114", 1L)).thenReturn(new RoomResolver.RoomResolveResult(RoomResolver.Status.RESOLVED, validRoom));
        when(showtimeRepository.findBySourceId("ncc:412427")).thenReturn(Optional.empty());
        // Overlap exists
        when(showtimeRepository.existsOverlappingShowtime(eq(10L), any(), any(), isNull())).thenReturn(true);
        when(showtimeRepository.findBySourceIdStartingWithAndIsActiveTrue("ncc:")).thenReturn(Collections.emptyList());

        ShowtimeCrawlerSummaryResponse summary = orchestrator.crawlShowtimes(1L);

        assertEquals(1, summary.getTotalFetched());
        assertEquals(0, summary.getInserted());
        assertEquals(1, summary.getSkipped());
        verify(showtimeRepository, never()).save(any());
    }

    @Test
    void crawlShowtimes_MissingPrices_ShouldApplyFallbackPrices() {
        ShowtimeItemDTO item = buildSampleItem("ncc:412427", futureTime);
        item.setPriceStandardRaw("");
        item.setPriceVipRaw(null);
        item.setPriceCoupleRaw("MALFORMED");

        when(crawlerClient.fetchShowtimeList()).thenReturn(List.of(item));
        when(crawlerClient.getName()).thenReturn("NCC");
        when(movieRepository.findBySourceId("ncc:11059")).thenReturn(Optional.of(validNowShowingMovie));
        when(roomResolver.resolve("2114", 1L)).thenReturn(new RoomResolver.RoomResolveResult(RoomResolver.Status.RESOLVED, validRoom));
        when(showtimeRepository.findBySourceId("ncc:412427")).thenReturn(Optional.empty());
        when(showtimeRepository.existsOverlappingShowtime(eq(10L), any(), any(), isNull())).thenReturn(false);
        when(showtimeRepository.findBySourceIdStartingWithAndIsActiveTrue("ncc:")).thenReturn(Collections.emptyList());

        ShowtimeCrawlerSummaryResponse summary = orchestrator.crawlShowtimes(1L);

        assertEquals(1, summary.getInserted());
        verify(showtimeRepository).save(argThat(st ->
                st.getPriceStandard().compareTo(new BigDecimal("90000")) == 0 &&
                st.getPriceVip().compareTo(new BigDecimal("95000")) == 0 &&
                st.getPriceCouple().compareTo(new BigDecimal("100000")) == 0
        ));
    }

    @Test
    void crawlShowtimes_SmartSync_Missing3TimesWithoutBooking_ShouldSoftDeactivate() {
        // Batch is empty (or does not contain the existing showtime)
        when(crawlerClient.fetchShowtimeList()).thenReturn(Collections.emptyList());
        when(crawlerClient.getName()).thenReturn("NCC");

        Showtime missingShowtime = Showtime.builder()
                .id(88L)
                .sourceId("ncc:412000")
                .movie(validNowShowingMovie)
                .room(validRoom)
                .startTime(futureTime)
                .endTime(futureTime.plusMinutes(110))
                .missingCount(2) // Missing twice previously, now becomes 3
                .isActive(true)
                .build();

        when(showtimeRepository.findBySourceIdStartingWithAndIsActiveTrue("ncc:")).thenReturn(List.of(missingShowtime));
        when(reservationRepository.existsByShowtimeId(88L)).thenReturn(false);

        ShowtimeCrawlerSummaryResponse summary = orchestrator.crawlShowtimes(1L);

        assertEquals(1, summary.getSoftDeactivated());
        assertEquals(3, missingShowtime.getMissingCount());
        assertFalse(missingShowtime.getIsActive());
        verify(showtimeRepository).save(missingShowtime);
    }

    @Test
    void crawlShowtimes_SmartSync_Missing3TimesWithBooking_ShouldProtectAndRemainActive() {
        when(crawlerClient.fetchShowtimeList()).thenReturn(Collections.emptyList());
        when(crawlerClient.getName()).thenReturn("NCC");

        Showtime bookedMissingShowtime = Showtime.builder()
                .id(99L)
                .sourceId("ncc:412001")
                .movie(validNowShowingMovie)
                .room(validRoom)
                .startTime(futureTime)
                .endTime(futureTime.plusMinutes(110))
                .missingCount(2) // Becomes 3
                .isActive(true)
                .build();

        when(showtimeRepository.findBySourceIdStartingWithAndIsActiveTrue("ncc:")).thenReturn(List.of(bookedMissingShowtime));
        // HAS ACTIVE RESERVATION
        when(reservationRepository.existsByShowtimeId(99L)).thenReturn(true);

        ShowtimeCrawlerSummaryResponse summary = orchestrator.crawlShowtimes(1L);

        assertEquals(0, summary.getSoftDeactivated());
        assertEquals(3, bookedMissingShowtime.getMissingCount());
        assertTrue(bookedMissingShowtime.getIsActive()); // Still protected!
        verify(showtimeRepository).save(bookedMissingShowtime);
    }
}
