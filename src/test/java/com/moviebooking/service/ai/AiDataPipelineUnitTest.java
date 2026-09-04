package com.moviebooking.service.ai;

import com.moviebooking.config.AiPipelineProperties;
import com.moviebooking.dto.ai.*;
import com.moviebooking.model.Article;
import com.moviebooking.model.Genre;
import com.moviebooking.model.Movie;
import com.moviebooking.model.Review;
import com.moviebooking.model.enums.*;
import com.moviebooking.service.ai.embedding.EmbeddingService;
import com.moviebooking.service.ai.embedding.MockEmbeddingProvider;
import com.moviebooking.service.ai.pipeline.chunker.DocumentChunker;
import com.moviebooking.service.ai.pipeline.transformer.DocumentTransformer;
import com.moviebooking.service.ai.pipeline.transformer.TextCleaner;
import com.moviebooking.service.ai.pipeline.validator.DocumentValidator;
import com.moviebooking.service.ai.vectorstore.InMemoryVectorStore;
import com.moviebooking.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử toàn diện cho AI Data Pipeline Phase 2.
 * Test thuần Java — không cần Spring Context, không cần Database, không cần Docker.
 */
@TestInstance(Lifecycle.PER_CLASS)
class AiDataPipelineUnitTest {

    private TextCleaner textCleaner;
    private DocumentTransformer transformer;
    private DocumentValidator validator;
    private DocumentChunker chunker;
    private EmbeddingService embeddingService;
    private InMemoryVectorStore vectorStore;

    @BeforeAll
    void setUp() {
        textCleaner = new TextCleaner();
        transformer = new DocumentTransformer(textCleaner);
        validator = new DocumentValidator();

        AiPipelineProperties props = new AiPipelineProperties();
        props.getPipeline().setMaxChunkSize(1500);
        props.getPipeline().setChunkOverlap(200);

        chunker = new DocumentChunker(props);

        MockEmbeddingProvider mockProvider = new MockEmbeddingProvider(props);
        embeddingService = new EmbeddingService(mockProvider);

        vectorStore = new InMemoryVectorStore();
        vectorStore.initializeCollection();
    }

    @BeforeEach
    void clearVectorStore() {
        vectorStore.clearCollection();
    }

    // ==================== TEXT CLEANER TESTS ====================

    @Nested
    @DisplayName("A. TextCleaner Tests")
    class TextCleanerTests {

        @Test
        @DisplayName("Loại bỏ HTML tags hoàn toàn")
        void shouldStripHtmlTags() {
            String html = "<p>Hello <b>World</b></p><br/><div>Test</div>";
            String cleaned = textCleaner.cleanHtml(html);
            assertFalse(cleaned.contains("<"), "Không được chứa HTML tags");
            assertTrue(cleaned.contains("Hello"), "Phải giữ nội dung text");
            assertTrue(cleaned.contains("World"), "Phải giữ nội dung text");
        }

        @Test
        @DisplayName("Decode HTML entities")
        void shouldDecodeHtmlEntities() {
            String html = "&amp; &lt; &gt; &quot;";
            String cleaned = textCleaner.cleanHtml(html);
            assertTrue(cleaned.contains("&"));
            assertTrue(cleaned.contains("<"));
        }

        @Test
        @DisplayName("Chuẩn hóa khoảng trắng thừa")
        void shouldNormalizeWhitespace() {
            String text = "Hello    World\n\n\n\nTest";
            String normalized = textCleaner.normalizeWhitespace(text);
            assertFalse(normalized.contains("    "), "Không được có nhiều khoảng trắng liên tiếp");
        }

        @Test
        @DisplayName("Xử lý input null/blank an toàn")
        void shouldHandleNullInput() {
            assertEquals("", textCleaner.cleanHtml(null));
            assertEquals("", textCleaner.cleanHtml(""));
            assertEquals("", textCleaner.cleanHtml("   "));
            assertEquals("", textCleaner.normalizeWhitespace(null));
        }
    }

    // ==================== MOVIE TRANSFORMATION TESTS ====================

    @Nested
    @DisplayName("B. Movie Transformation Tests")
    class MovieTransformationTests {

        @Test
        @DisplayName("Chuyển đổi Movie đầy đủ thành KnowledgeDocument")
        void shouldTransformMovieWithAllFields() {
            Movie movie = buildTestMovie();
            KnowledgeDocument doc = transformer.transformMovie(movie);

            assertEquals("doc:movie:1", doc.getDocumentId());
            assertEquals(SourceType.MOVIE, doc.getSourceType());
            assertEquals(1L, doc.getSourceId());
            assertEquals(1L, doc.getMovieId());
            assertEquals("Interstellar", doc.getTitle());

            // Kiểm tra nội dung chứa đủ metadata quan trọng
            String content = doc.getContent();
            assertTrue(content.contains("[MOVIE KNOWLEDGE]"));
            assertTrue(content.contains("Interstellar"));
            assertTrue(content.contains("Christopher Nolan"));
            assertTrue(content.contains("Science Fiction"));
            assertTrue(content.contains("Matthew McConaughey"));
            assertTrue(content.contains("169 minutes"));
            assertTrue(content.contains("T13"));
            assertTrue(content.contains("Synopsis:"));
        }

        @Test
        @DisplayName("Content hash phải tất định")
        void shouldGenerateDeterministicContentHash() {
            Movie movie = buildTestMovie();
            KnowledgeDocument doc1 = transformer.transformMovie(movie);
            KnowledgeDocument doc2 = transformer.transformMovie(movie);
            assertEquals(doc1.getContentHash(), doc2.getContentHash());
        }

        @Test
        @DisplayName("Movie không có createdAt/updatedAt")
        void shouldHandleMissingTimestamps() {
            Movie movie = buildTestMovie();
            KnowledgeDocument doc = transformer.transformMovie(movie);
            assertNull(doc.getSourceCreatedAt());
            assertNull(doc.getSourceUpdatedAt());
        }
    }

    // ==================== REVIEW TRANSFORMATION TESTS ====================

    @Nested
    @DisplayName("C. Review Transformation Tests")
    class ReviewTransformationTests {

        @Test
        @DisplayName("Chuyển đổi Review đã PUBLISHED — Zero PII")
        void shouldTransformPublishedReviewWithoutPII() {
            Review review = buildTestReview(ReviewStatus.PUBLISHED);
            KnowledgeDocument doc = transformer.transformReview(review);

            String content = doc.getContent();

            // Kiểm tra Fact vs Opinion format
            assertTrue(content.contains("[AUDIENCE REVIEW]"));
            assertTrue(content.contains("Rating: 5/5 stars"));
            assertTrue(content.contains("Verified Ticket Buyer: Yes"));
            assertTrue(content.contains("Review Comment:"));
            assertTrue(content.contains("Amazing movie"));

            // CRITICAL: Zero PII
            assertFalse(content.contains("test@email.com"), "KHÔNG ĐƯỢC chứa email");
            assertFalse(content.contains("John"), "KHÔNG ĐƯỢC chứa tên user");
            assertFalse(content.contains("Doe"), "KHÔNG ĐƯỢC chứa họ user");
            assertFalse(content.contains("0123456789"), "KHÔNG ĐƯỢC chứa SĐT");
            assertFalse(content.contains("password"), "KHÔNG ĐƯỢC chứa mật khẩu");
        }

        @Test
        @DisplayName("Review metadata chứa đúng thông tin cho filtering")
        void shouldPreserveFilterMetadata() {
            Review review = buildTestReview(ReviewStatus.PUBLISHED);
            KnowledgeDocument doc = transformer.transformReview(review);

            assertEquals(5, doc.getMetadata().get("rating"));
            assertEquals(true, doc.getMetadata().get("verifiedPurchase"));
            assertEquals("Interstellar", doc.getMetadata().get("movieTitle"));
        }

        @Test
        @DisplayName("Phân biệt rõ Fact vs Opinion trong Review")
        void shouldSeparateFactFromOpinion() {
            Review review = buildTestReview(ReviewStatus.PUBLISHED);
            KnowledgeDocument doc = transformer.transformReview(review);

            String content = doc.getContent();
            // Rating là FACT
            assertTrue(content.contains("Rating: 5/5 stars"));
            // Verified purchase là FACT
            assertTrue(content.contains("Verified Ticket Buyer: Yes"));
            // Comment là OPINION — nằm dưới label rõ ràng
            assertTrue(content.contains("Review Comment:"));
        }
    }

    // ==================== ARTICLE TRANSFORMATION TESTS ====================

    @Nested
    @DisplayName("D. Article Transformation Tests")
    class ArticleTransformationTests {

        @Test
        @DisplayName("Chuyển đổi Article và strip HTML")
        void shouldTransformArticleWithHtmlStripping() {
            Article article = buildTestArticle(ArticleStatus.PUBLISHED);
            article.setContent("<p>This is <b>bold</b> content</p><script>alert('xss')</script>");

            KnowledgeDocument doc = transformer.transformArticle(article);

            String content = doc.getContent();
            assertTrue(content.contains("[ARTICLE:"));
            assertTrue(content.contains("Test Article Title"));
            assertFalse(content.contains("<p>"), "HTML tags phải được loại bỏ");
            assertFalse(content.contains("<b>"), "HTML tags phải được loại bỏ");
            assertFalse(content.contains("<script>"), "Script tags phải được loại bỏ");
            assertTrue(content.contains("bold"));
            assertTrue(content.contains("content"));
        }
    }

    // ==================== VALIDATION TESTS ====================

    @Nested
    @DisplayName("E. Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Tài liệu hợp lệ qua validation")
        void shouldAcceptValidDocument() {
            KnowledgeDocument doc = transformer.transformMovie(buildTestMovie());
            assertTrue(validator.isValid(doc));
        }

        @Test
        @DisplayName("Từ chối nội dung rỗng")
        void shouldRejectBlankContent() {
            KnowledgeDocument doc = KnowledgeDocument.builder()
                    .documentId("doc:movie:1")
                    .sourceType(SourceType.MOVIE)
                    .sourceId(1L)
                    .title("Test")
                    .content("")
                    .build();
            assertFalse(validator.isValid(doc));
        }

        @Test
        @DisplayName("Từ chối tiêu đề rỗng cho Movie")
        void shouldRejectBlankTitleForMovie() {
            KnowledgeDocument doc = KnowledgeDocument.builder()
                    .documentId("doc:movie:1")
                    .sourceType(SourceType.MOVIE)
                    .sourceId(1L)
                    .title("")
                    .content("Some content")
                    .build();
            assertFalse(validator.isValid(doc));
        }

        @Test
        @DisplayName("Từ chối rating ngoài khoảng 1-5")
        void shouldRejectInvalidRating() {
            Map<String, Object> meta = new HashMap<>();
            meta.put("rating", 6);
            KnowledgeDocument doc = KnowledgeDocument.builder()
                    .documentId("doc:review:1")
                    .sourceType(SourceType.REVIEW)
                    .sourceId(1L)
                    .content("Some review")
                    .metadata(meta)
                    .build();
            assertFalse(validator.isValid(doc));
        }

        @Test
        @DisplayName("Chấp nhận rating hợp lệ 1-5")
        void shouldAcceptValidRating() {
            for (int rating = 1; rating <= 5; rating++) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("rating", rating);
                KnowledgeDocument doc = KnowledgeDocument.builder()
                        .documentId("doc:review:" + rating)
                        .sourceType(SourceType.REVIEW)
                        .sourceId((long) rating)
                        .content("Review content")
                        .metadata(meta)
                        .build();
                assertTrue(validator.isValid(doc), "Rating " + rating + " phải hợp lệ");
            }
        }

        @Test
        @DisplayName("Từ chối document null")
        void shouldRejectNullDocument() {
            assertFalse(validator.isValid(null));
        }
    }

    // ==================== CHUNKING TESTS ====================

    @Nested
    @DisplayName("F. Chunking Tests")
    class ChunkingTests {

        @Test
        @DisplayName("Movie tạo đúng 1 chunk")
        void movieShouldProduceSingleChunk() {
            KnowledgeDocument doc = transformer.transformMovie(buildTestMovie());
            List<KnowledgeChunk> chunks = chunker.chunk(doc);
            assertEquals(1, chunks.size());
            assertEquals("movie_1", chunks.getFirst().getChunkId());
            assertEquals(0, chunks.getFirst().getChunkIndex());
            assertEquals(1, chunks.getFirst().getTotalChunks());
        }

        @Test
        @DisplayName("Review tạo đúng 1 chunk")
        void reviewShouldProduceSingleChunk() {
            KnowledgeDocument doc = transformer.transformReview(buildTestReview(ReviewStatus.PUBLISHED));
            List<KnowledgeChunk> chunks = chunker.chunk(doc);
            assertEquals(1, chunks.size());
            assertEquals("review_42", chunks.getFirst().getChunkId());
        }

        @Test
        @DisplayName("Article ngắn tạo 1 chunk")
        void shortArticleShouldProduceSingleChunk() {
            Article article = buildTestArticle(ArticleStatus.PUBLISHED);
            article.setContent("Short content.");
            KnowledgeDocument doc = transformer.transformArticle(article);
            List<KnowledgeChunk> chunks = chunker.chunk(doc);
            assertEquals(1, chunks.size());
            assertTrue(chunks.getFirst().getChunkId().startsWith("article_10"));
        }

        @Test
        @DisplayName("Article dài tạo nhiều chunks — mỗi chunk có Context Header")
        void longArticleShouldProduceMultipleChunks() {
            Article article = buildTestArticle(ArticleStatus.PUBLISHED);
            // Tạo nội dung rất dài (> 3000 ký tự)
            StringBuilder longContent = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                longContent.append("Paragraph ").append(i)
                        .append(": This is a sufficiently long paragraph of text that discusses ")
                        .append("various aspects of Vietnamese cinema and its cultural impact. ")
                        .append("The movie industry has grown significantly in recent years.\n\n");
            }
            article.setContent(longContent.toString());

            KnowledgeDocument doc = transformer.transformArticle(article);
            List<KnowledgeChunk> chunks = chunker.chunk(doc);

            assertTrue(chunks.size() > 1, "Article dài phải tạo > 1 chunk");

            // Kiểm tra mỗi chunk đều có Context Header
            for (KnowledgeChunk chunk : chunks) {
                assertTrue(chunk.getText().contains("[ARTICLE:"),
                        "Mỗi chunk phải có [ARTICLE: header");
                assertTrue(chunk.getText().contains("Test Article Title"),
                        "Mỗi chunk phải có tiêu đề bài viết");
                assertEquals(SourceType.ARTICLE, chunk.getSourceType());
                assertEquals(10L, chunk.getSourceId());
            }

            // Kiểm tra chunkIndex liên tục
            for (int i = 0; i < chunks.size(); i++) {
                assertEquals(i, chunks.get(i).getChunkIndex());
                assertEquals(chunks.size(), chunks.get(i).getTotalChunks());
            }
        }

        @Test
        @DisplayName("Chunk IDs phải tất định")
        void chunkIdsMustBeDeterministic() {
            KnowledgeDocument doc = transformer.transformMovie(buildTestMovie());
            List<KnowledgeChunk> chunks1 = chunker.chunk(doc);
            List<KnowledgeChunk> chunks2 = chunker.chunk(doc);
            assertEquals(chunks1.getFirst().getChunkId(), chunks2.getFirst().getChunkId());
        }
    }

    // ==================== EMBEDDING TESTS ====================

    @Nested
    @DisplayName("G. Embedding Tests")
    class EmbeddingTests {

        @Test
        @DisplayName("Mock embedding tạo vector đúng dimension")
        void shouldGenerateCorrectDimension() {
            float[] embedding = embeddingService.generateEmbedding("test");
            assertEquals(768, embedding.length);
        }

        @Test
        @DisplayName("Mock embedding tạo vector tất định")
        void shouldBeDeterministic() {
            float[] e1 = embeddingService.generateEmbedding("same text");
            float[] e2 = embeddingService.generateEmbedding("same text");
            assertArrayEquals(e1, e2);
        }

        @Test
        @DisplayName("embedChunks gán vector cho mỗi chunk")
        void shouldEmbedAllChunks() {
            KnowledgeDocument doc = transformer.transformMovie(buildTestMovie());
            List<KnowledgeChunk> chunks = chunker.chunk(doc);
            assertNull(chunks.getFirst().getEmbedding());

            embeddingService.embedChunks(chunks);

            assertNotNull(chunks.getFirst().getEmbedding());
            assertEquals(768, chunks.getFirst().getEmbedding().length);
        }
    }

    // ==================== VECTOR STORE TESTS ====================

    @Nested
    @DisplayName("H. InMemoryVectorStore Tests")
    class VectorStoreTests {

        @Test
        @DisplayName("Upsert và count hoạt động")
        void shouldUpsertAndCount() {
            KnowledgeDocument doc = transformer.transformMovie(buildTestMovie());
            List<KnowledgeChunk> chunks = chunker.chunk(doc);
            embeddingService.embedChunks(chunks);

            vectorStore.upsert(chunks);
            assertEquals(1, vectorStore.count());
            assertTrue(vectorStore.containsVector("movie_1"));
        }

        @Test
        @DisplayName("Idempotent: upsert 2 lần không tạo duplicate")
        void shouldBeIdempotent() {
            KnowledgeDocument doc = transformer.transformMovie(buildTestMovie());
            List<KnowledgeChunk> chunks = chunker.chunk(doc);
            embeddingService.embedChunks(chunks);

            vectorStore.upsert(chunks);
            vectorStore.upsert(chunks);

            assertEquals(1, vectorStore.count(), "Upsert 2 lần phải giữ đúng 1 vector");
        }

        @Test
        @DisplayName("deleteBySource xóa đúng source")
        void shouldDeleteBySource() {
            // Insert movie
            KnowledgeDocument movieDoc = transformer.transformMovie(buildTestMovie());
            List<KnowledgeChunk> movieChunks = chunker.chunk(movieDoc);
            embeddingService.embedChunks(movieChunks);
            vectorStore.upsert(movieChunks);

            // Insert review
            KnowledgeDocument reviewDoc = transformer.transformReview(buildTestReview(ReviewStatus.PUBLISHED));
            List<KnowledgeChunk> reviewChunks = chunker.chunk(reviewDoc);
            embeddingService.embedChunks(reviewChunks);
            vectorStore.upsert(reviewChunks);

            assertEquals(2, vectorStore.count());

            // Xóa review
            vectorStore.deleteBySource(SourceType.REVIEW, 42L);
            assertEquals(1, vectorStore.count());
            assertTrue(vectorStore.containsVector("movie_1"));
            assertFalse(vectorStore.containsVector("review_42"));
        }

        @Test
        @DisplayName("countBySourceType hoạt động đúng")
        void shouldCountBySourceType() {
            // Insert movie and review
            KnowledgeDocument movieDoc = transformer.transformMovie(buildTestMovie());
            List<KnowledgeChunk> movieChunks = chunker.chunk(movieDoc);
            embeddingService.embedChunks(movieChunks);
            vectorStore.upsert(movieChunks);

            KnowledgeDocument reviewDoc = transformer.transformReview(buildTestReview(ReviewStatus.PUBLISHED));
            List<KnowledgeChunk> reviewChunks = chunker.chunk(reviewDoc);
            embeddingService.embedChunks(reviewChunks);
            vectorStore.upsert(reviewChunks);

            Map<String, Long> counts = vectorStore.countBySourceType();
            assertEquals(1L, counts.get("MOVIE"));
            assertEquals(1L, counts.get("REVIEW"));
        }

        @Test
        @DisplayName("clearCollection xóa toàn bộ")
        void shouldClearAll() {
            KnowledgeDocument doc = transformer.transformMovie(buildTestMovie());
            List<KnowledgeChunk> chunks = chunker.chunk(doc);
            embeddingService.embedChunks(chunks);
            vectorStore.upsert(chunks);

            assertEquals(1, vectorStore.count());
            vectorStore.clearCollection();
            assertEquals(0, vectorStore.count());
        }

        @Test
        @DisplayName("Vector store luôn available (in-memory)")
        void shouldAlwaysBeAvailable() {
            assertTrue(vectorStore.isAvailable());
        }
    }

    // ==================== CONTENT HASH TESTS ====================

    @Nested
    @DisplayName("I. Content Hash / Determinism Tests")
    class DeterminismTests {

        @Test
        @DisplayName("Cùng input cho cùng document ID")
        void sameInputSameDocumentId() {
            Movie movie = buildTestMovie();
            KnowledgeDocument doc1 = transformer.transformMovie(movie);
            KnowledgeDocument doc2 = transformer.transformMovie(movie);
            assertEquals(doc1.getDocumentId(), doc2.getDocumentId());
        }

        @Test
        @DisplayName("Cùng input cho cùng content hash")
        void sameInputSameContentHash() {
            Movie movie = buildTestMovie();
            KnowledgeDocument doc1 = transformer.transformMovie(movie);
            KnowledgeDocument doc2 = transformer.transformMovie(movie);
            assertEquals(doc1.getContentHash(), doc2.getContentHash());
        }

        @Test
        @DisplayName("Nội dung khác cho hash khác")
        void differentContentDifferentHash() {
            Movie movie1 = buildTestMovie();
            Movie movie2 = buildTestMovie();
            movie2.setDescription("A completely different synopsis");

            KnowledgeDocument doc1 = transformer.transformMovie(movie1);
            KnowledgeDocument doc2 = transformer.transformMovie(movie2);
            assertNotEquals(doc1.getContentHash(), doc2.getContentHash());
        }
    }

    // ==================== LIFECYCLE TESTS ====================

    @Nested
    @DisplayName("J. Lifecycle Handling Tests")
    class LifecycleTests {

        @Test
        @DisplayName("HIDDEN review → vector bị xóa")
        void hiddenReviewShouldBeRemoved() {
            // Index a published review
            KnowledgeDocument doc = transformer.transformReview(buildTestReview(ReviewStatus.PUBLISHED));
            List<KnowledgeChunk> chunks = chunker.chunk(doc);
            embeddingService.embedChunks(chunks);
            vectorStore.upsert(chunks);
            assertTrue(vectorStore.containsVector("review_42"));

            // Simulate HIDDEN → delete from index
            vectorStore.deleteBySource(SourceType.REVIEW, 42L);
            assertFalse(vectorStore.containsVector("review_42"));
        }

        @Test
        @DisplayName("Article multi-chunk update → old chunks cleaned")
        void articleUpdateShouldCleanOldChunks() {
            // Index article with long content → multiple chunks
            Article article = buildTestArticle(ArticleStatus.PUBLISHED);
            StringBuilder longContent = new StringBuilder();
            for (int i = 0; i < 30; i++) {
                longContent.append("Long paragraph ").append(i).append(": Lorem ipsum dolor sit amet. ");
                longContent.append("This is content that needs to be chunked into multiple pieces.\n\n");
            }
            article.setContent(longContent.toString());

            KnowledgeDocument doc = transformer.transformArticle(article);
            List<KnowledgeChunk> chunks = chunker.chunk(doc);
            embeddingService.embedChunks(chunks);
            vectorStore.upsert(chunks);

            long initialCount = vectorStore.count();
            assertTrue(initialCount >= 1);

            // Simulate update: delete old → insert new
            vectorStore.deleteBySource(SourceType.ARTICLE, 10L);
            assertEquals(0, vectorStore.count());

            // Re-index with shorter content
            article.setContent("Short updated content.");
            KnowledgeDocument updatedDoc = transformer.transformArticle(article);
            List<KnowledgeChunk> newChunks = chunker.chunk(updatedDoc);
            embeddingService.embedChunks(newChunks);
            vectorStore.upsert(newChunks);

            assertEquals(1, vectorStore.count(), "Updated short article phải chỉ có 1 chunk");
        }
    }

    // ==================== FULL PIPELINE INTEGRATION TEST ====================

    @Nested
    @DisplayName("K. End-to-End Pipeline Test")
    class EndToEndTests {

        @Test
        @DisplayName("Full pipeline: Movie → Transform → Validate → Chunk → Embed → Index")
        void fullPipelineForMovie() {
            Movie movie = buildTestMovie();

            // Transform
            KnowledgeDocument doc = transformer.transformMovie(movie);
            assertNotNull(doc);
            assertEquals(SourceType.MOVIE, doc.getSourceType());

            // Validate
            assertTrue(validator.isValid(doc));

            // Chunk
            List<KnowledgeChunk> chunks = chunker.chunk(doc);
            assertEquals(1, chunks.size());

            // Embed
            embeddingService.embedChunks(chunks);
            assertNotNull(chunks.getFirst().getEmbedding());

            // Index
            vectorStore.upsert(chunks);
            assertEquals(1, vectorStore.count());
            assertTrue(vectorStore.containsVector("movie_1"));

            // Verify metadata payload
            Map<String, Object> meta = vectorStore.getVectorMetadata("movie_1");
            assertNotNull(meta);
            assertEquals("MOVIE", meta.get("sourceType"));
            assertEquals(1L, meta.get("sourceId"));
        }
    }

    // ==================== TEST DATA BUILDERS ====================

    private Movie buildTestMovie() {
        Set<Genre> genres = new HashSet<>();
        Genre g1 = new Genre();
        g1.setId(1L);
        g1.setName("Science Fiction");
        Genre g2 = new Genre();
        g2.setId(2L);
        g2.setName("Drama");
        genres.add(g1);
        genres.add(g2);

        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Interstellar");
        movie.setTitleEn("Interstellar");
        movie.setDescription("A team of explorers travel through a wormhole in space.");
        movie.setDirector("Christopher Nolan");
        movie.setActors("Matthew McConaughey, Anne Hathaway, Jessica Chastain");
        movie.setDuration(169);
        movie.setReleaseDate(LocalDate.of(2024, 11, 7));
        movie.setEndDate(LocalDate.of(2025, 2, 28));
        movie.setAgeRating(AgeRating.T13);
        movie.setLanguage("English");
        movie.setSubtitle("Vietnamese");
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setGenres(genres);
        movie.setPosterPath("/posters/interstellar.jpg");
        return movie;
    }

    private Review buildTestReview(ReviewStatus status) {
        User user = new User();
        user.setId(99L);
        user.setEmail("test@email.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhone("0123456789");
        user.setPassword("hashedPassword123");

        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Interstellar");

        Review review = new Review();
        review.setId(42L);
        review.setUser(user);
        review.setMovie(movie);
        review.setRating(5);
        review.setComment("Amazing movie with incredible visual effects!");
        review.setVerifiedPurchase(true);
        review.setStatus(status);
        review.setCreatedAt(LocalDateTime.of(2026, 9, 1, 10, 30));
        review.setUpdatedAt(LocalDateTime.of(2026, 9, 1, 10, 30));
        return review;
    }

    private Article buildTestArticle(ArticleStatus status) {
        Article article = new Article();
        article.setId(10L);
        article.setTitle("Test Article Title");
        article.setShortDescription("A short description of the article.");
        article.setContent("Full article content here.");
        article.setStatus(status);
        article.setCreatedAt(LocalDateTime.of(2026, 9, 1, 8, 0));
        article.setUpdatedAt(LocalDateTime.of(2026, 9, 2, 10, 0));
        return article;
    }
}
