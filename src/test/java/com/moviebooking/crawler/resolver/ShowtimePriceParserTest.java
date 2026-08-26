package com.moviebooking.crawler.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ShowtimePriceParserTest {

    private ShowtimePriceParser parser;

    @BeforeEach
    void setUp() {
        parser = new ShowtimePriceParser();
    }

    // =========================================================================
    // Single price parsing
    // =========================================================================

    @Test
    void parse_WithPrefixedPrice_ShouldExtractDigits() {
        assertEquals(new BigDecimal("90000"), parser.parse("T:90000", "Standard"));
        assertEquals(new BigDecimal("95000"), parser.parse("V:95000", "VIP"));
        assertEquals(new BigDecimal("100000"), parser.parse("D:100000", "Couple"));
    }

    @Test
    void parse_WithPlainDigits_ShouldParse() {
        assertEquals(new BigDecimal("90000"), parser.parse("90000", "Standard"));
        assertEquals(new BigDecimal("150000"), parser.parse("150000", "VIP"));
    }

    @Test
    void parse_WithNull_ShouldReturnNull() {
        assertNull(parser.parse(null, "Standard"));
    }

    @Test
    void parse_WithBlank_ShouldReturnNull() {
        assertNull(parser.parse("", "Standard"));
        assertNull(parser.parse("   ", "VIP"));
    }

    @Test
    void parse_WithMalformedPrice_ShouldReturnNull() {
        assertNull(parser.parse("ABC", "Standard"));
        assertNull(parser.parse("T:ABC", "Standard"));
        assertNull(parser.parse("T:", "Standard"));
    }

    @Test
    void parse_WithZeroPrice_ShouldReturnNull() {
        assertNull(parser.parse("0", "Standard"));
        assertNull(parser.parse("T:0", "Standard"));
    }

    // =========================================================================
    // Batch price parsing (parseAll)
    // =========================================================================

    @Test
    void parseAll_WithAllValidPrices_ShouldReturnComplete() {
        ShowtimePriceParser.PriceResult result = parser.parseAll("T:90000", "V:95000", "D:100000");

        assertTrue(result.isComplete());
        assertFalse(result.hasNulls());
        assertEquals(new BigDecimal("90000"), result.standard());
        assertEquals(new BigDecimal("95000"), result.vip());
        assertEquals(new BigDecimal("100000"), result.couple());
    }

    @Test
    void parseAll_WithSomeMissing_ShouldReturnIncomplete() {
        ShowtimePriceParser.PriceResult result = parser.parseAll("T:90000", "", "D:100000");

        assertFalse(result.isComplete());
        assertTrue(result.hasNulls());
        assertEquals(new BigDecimal("90000"), result.standard());
        assertNull(result.vip());
        assertEquals(new BigDecimal("100000"), result.couple());
    }

    @Test
    void parseAll_WithAllNull_ShouldReturnAllNulls() {
        ShowtimePriceParser.PriceResult result = parser.parseAll(null, null, null);

        assertFalse(result.isComplete());
        assertTrue(result.hasNulls());
        assertNull(result.standard());
        assertNull(result.vip());
        assertNull(result.couple());
    }
}
