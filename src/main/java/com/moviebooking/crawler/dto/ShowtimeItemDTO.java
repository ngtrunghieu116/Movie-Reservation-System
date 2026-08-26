package com.moviebooking.crawler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Raw Showtime / Session DTO extracted from NCC website RSC payload.
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeItemDTO {
    // Unique source identifiers
    private String sourceId;      // e.g. "ncc:412427"
    private String filmSourceId;  // e.g. "ncc:11059"
    private String filmTitle;     // e.g. "NGHỈ HÈ SỢ NGHỈ HƯU"
    private String roomSourceId;  // e.g. "2114"
    
    // Time
    private LocalDateTime startTime; // parsed from "ProjectTime" (e.g. "2026-08-22T18:55:00")
    
    // Raw Prices from NCC
    private String priceStandardRaw; // e.g. "T:90000" (Position 2)
    private String priceVipRaw;      // e.g. "V:95000" (Position 3)
    private String priceCoupleRaw;   // e.g. "D:100000" (Position 1)
    
    // Status flags from NCC
    private Boolean isOnlineSelling; // true if IsOnlineSelling == 1 and Deleted == false
    private Boolean deleted;
}
