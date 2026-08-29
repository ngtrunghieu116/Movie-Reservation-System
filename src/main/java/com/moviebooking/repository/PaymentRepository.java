package com.moviebooking.repository;

import com.moviebooking.model.Payment;
import com.moviebooking.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReservationId(Long reservationId);

    Optional<Payment> findByTransactionRef(String transactionRef);

    Optional<Payment> findByTransactionNo(String transactionNo);

    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByTransactionRef(String transactionRef);
}
