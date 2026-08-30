package com.moviebooking.controller;

import com.moviebooking.dto.res.CreatePaymentResponse;
import com.moviebooking.dto.res.ReservationReviewResponse;
import com.moviebooking.model.User;
import com.moviebooking.security.SecurityUtils;
import com.moviebooking.service.payment.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final SecurityUtils securityUtils;

    @PostMapping("/{reservationId}/create")
    public ResponseEntity<CreatePaymentResponse> createPaymentUrl(
            @PathVariable Long reservationId,
            HttpServletRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        String clientIp = com.moviebooking.util.VnPayUtil.getIpAddress(request);
        return ResponseEntity.ok(paymentService.createPaymentUrl(reservationId, currentUser, clientIp));
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<ReservationReviewResponse> processVnPayReturn(
            @RequestParam Map<String, String> queryParams) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(paymentService.processVnPayReturn(queryParams, currentUser));
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> processVnPayIpn(
            @RequestParam Map<String, String> queryParams) {
        Map<String, String> result = paymentService.processVnPayIpn(queryParams);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<com.moviebooking.dto.res.PaymentStatusDetailResponse> getPaymentStatus(
            @RequestParam("orderId") String orderId) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(paymentService.getPaymentStatusDetail(orderId, currentUser));
    }
}
