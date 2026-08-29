package com.moviebooking.repository;

import com.moviebooking.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByReservationId(Long reservationId);

    void deleteByReservationId(Long reservationId);
}
