package com.moviebooking.service.payment;

import com.moviebooking.model.Payment;
import com.moviebooking.model.enums.PaymentStatus;
import com.moviebooking.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailureHandler {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPaymentFailure(Long paymentId, String transactionNo, String bankCode, String reason) {
        paymentRepository.findById(paymentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            if (transactionNo != null) payment.setTransactionNo(transactionNo);
            if (bankCode != null) payment.setBankCode(bankCode);
            paymentRepository.save(payment);
            log.info("[PAYMENT_FAILURE_RECORDED] Payment ID {} marked FAILED in independent transaction. Reason: {}", paymentId, reason);
        });
    }
}
