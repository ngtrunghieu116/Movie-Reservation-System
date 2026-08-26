package com.moviebooking.crawler.resolver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw NCC price strings into BigDecimal values.
 *
 * NCC price format examples:
 * - "T:90000"    → 90000 (Standard seat)
 * - "V:95000"    → 95000 (VIP seat)
 * - "D:100000"   → 100000 (Couple seat / Đôi)
 * - "90000"      → 90000 (no prefix)
 * - ""           → null (empty)
 * - null         → null
 *
 * Business Rule (RULE-10):
 * - Standard mapping: T → Standard, V → VIP, D → Couple.
 * - If raw price is null/empty/malformed → return null (caller uses fallback).
 * - No genre-based pricing.
 */
@Slf4j
@Component
public class ShowtimePriceParser {

    // Matches optional prefix letter(s) + colon, then digits
    private static final Pattern PRICE_PATTERN = Pattern.compile("^(?:[A-Za-z]+:)?\\s*(\\d+)$");

    /**
     * Parses a raw NCC price string to BigDecimal.
     *
     * @param rawPrice The raw price string from NCC (e.g. "T:90000", "90000", "", null)
     * @param label    A label for logging purposes (e.g. "Standard", "VIP", "Couple")
     * @return BigDecimal value or null if unparseable
     */
    public BigDecimal parse(String rawPrice, String label) {
        if (rawPrice == null || rawPrice.isBlank()) {
            log.debug("[PRICE_PARSE] {} price is null/blank, returning null for fallback", label);
            return null;
        }

        String trimmed = rawPrice.trim();
        Matcher matcher = PRICE_PATTERN.matcher(trimmed);

        if (matcher.matches()) {
            String digits = matcher.group(1);
            try {
                BigDecimal price = new BigDecimal(digits);
                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("[PRICE_PARSE] {} price is zero or negative raw='{}', returning null", label, rawPrice);
                    return null;
                }
                return price;
            } catch (NumberFormatException e) {
                log.warn("[PRICE_PARSE] {} price digits not parseable raw='{}': {}", label, rawPrice, e.getMessage());
                return null;
            }
        }

        log.warn("[PRICE_PARSE] {} price format unrecognized raw='{}'", label, rawPrice);
        return null;
    }

    /**
     * Parses all 3 NCC price fields into a PriceResult record.
     *
     * @param priceStandardRaw Raw Standard price (PriceOfPosition2)
     * @param priceVipRaw      Raw VIP price (PriceOfPosition3)
     * @param priceCoupleRaw   Raw Couple price (PriceOfPosition1)
     * @return PriceResult with parsed values (any may be null)
     */
    public PriceResult parseAll(String priceStandardRaw, String priceVipRaw, String priceCoupleRaw) {
        BigDecimal standard = parse(priceStandardRaw, "Standard");
        BigDecimal vip = parse(priceVipRaw, "VIP");
        BigDecimal couple = parse(priceCoupleRaw, "Couple");
        return new PriceResult(standard, vip, couple);
    }

    public record PriceResult(BigDecimal standard, BigDecimal vip, BigDecimal couple) {
        /**
         * Returns true if all 3 prices are successfully parsed (non-null).
         */
        public boolean isComplete() {
            return standard != null && vip != null && couple != null;
        }

        /**
         * Returns true if at least one price is missing (null).
         */
        public boolean hasNulls() {
            return standard == null || vip == null || couple == null;
        }
    }
}
