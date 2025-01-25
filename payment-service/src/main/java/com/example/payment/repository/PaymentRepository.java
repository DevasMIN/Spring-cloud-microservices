package com.example.payment.repository;

import com.example.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId ORDER BY p.timestamp DESC LIMIT 1")
    Optional<Payment> findByOrderId(Long orderId);

    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.status = 'COMPLETED' ORDER BY p.timestamp DESC")
    Optional<Payment> findSuccessfulPaymentByOrderId(Long orderId);
}
