package com.moviebooking.dto.res;

import com.moviebooking.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentResponse {
    private Long reservationId;
    private Long paymentId;
    private String transactionRef;
    private BigDecimal amount;
    private String paymentUrl;
    private PaymentStatus status;
}
