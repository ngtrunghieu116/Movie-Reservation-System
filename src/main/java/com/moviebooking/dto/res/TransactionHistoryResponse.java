package com.moviebooking.dto.res;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionHistoryResponse {
    private LocalDateTime transactionDate;
    private String movieTitle;
    private String transactionType;
    private Integer ticketCount;
    private BigDecimal totalAmount;
}
