package com.example.payment.service;

import com.example.payment.dto.BalanceDTO;
import com.example.payment.enums.PaymentStatus;
import com.example.payment.exceptions.InsufficientFundsException;
import com.example.payment.exceptions.PaymentException;
import com.example.payment.model.Payment;
import com.example.payment.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BalanceService balanceService;

    @Transactional
    public boolean processPayment(Long orderId, Long userId, BigDecimal amount) {
        log.info("Processing payment for order: {}, user: {}, amount: {}", orderId, userId, amount);

        Optional<Payment> existingPayment = paymentRepository.findSuccessfulPaymentByOrderId(orderId);
        if (existingPayment.isPresent()) {
            log.warn("Payment already exists for order: {}", orderId);
            throw new PaymentException("Payment already processed for this order");
        }

        // Проверяем баланс
        BalanceDTO balance = balanceService.getBalance(userId);
        if (balance.getAmount().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        // Создаем запись о платеже
        Payment payment = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .transactionId(generateTransactionId())
                .timestamp(LocalDateTime.now())
                .build();

        try {
            // Имитация времени на обработку платежа
            imitatePaymentProcessing();

            // Проверяем баланс и списываем средства
            boolean paymentSuccess = balanceService.processPayment(userId, amount);
            
            if (paymentSuccess) {
                payment.setStatus(PaymentStatus.COMPLETED);
                paymentRepository.save(payment);
                log.debug("Payment processed successfully for order: {}", orderId);
                return true;
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Insufficient funds");
                paymentRepository.save(payment);
                log.error("Payment failed - insufficient funds for order: {}", orderId);
                return false;
            }
        } catch (EntityNotFoundException e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("User not found");
            paymentRepository.save(payment);
            log.error("Payment failed - user not found: {}", userId);
            return false;
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);
            log.error("Payment processing failed for order: {}", orderId, e);
            return false;
        }
    }

    @Transactional
    public void refundPayment(Long orderId, Long userId, BigDecimal amount) {
        log.info("Processing refund for order: {}, user: {}, amount: {}", orderId, userId, amount);
        
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
        
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            log.warn("Cannot refund payment for order {} - payment status is {}", orderId, payment.getStatus());
            return;
        }

        try {
            balanceService.refundPayment(userId, amount);
            
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundTimestamp(LocalDateTime.now());
            paymentRepository.save(payment);
            
            log.debug("Refund processed successfully for order: {}", orderId);
        } catch (Exception e) {
            log.error("Refund processing failed for order: {}", orderId, e);
            throw e; // Пробрасываем исключение для отката транзакции
        }
    }

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
    }


    public void imitatePaymentProcessing() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.error("Error imitating payment processing", e);
        }
    }
}
