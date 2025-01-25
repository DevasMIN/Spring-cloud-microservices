package com.example.payment.service;

import com.example.payment.dto.BalanceDTO;
import com.example.payment.enums.PaymentStatus;
import com.example.payment.model.Payment;
import com.example.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BalanceService balanceService;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessPayment_Success() {
        // Настраиваем баланс
        BalanceDTO balanceDTO = new BalanceDTO(1L, BigDecimal.valueOf(1000));
        when(balanceService.getBalance(1L)).thenReturn(balanceDTO);

        // Настраиваем успешное списание средств
        when(balanceService.processPayment(1L, BigDecimal.valueOf(500))).thenReturn(true);

        // Настраиваем сохранение платежа
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });

        boolean result = paymentService.processPayment(10L, 1L, BigDecimal.valueOf(500));
        assertTrue(result);

        // Проверяем, что Payment создан и сохранён со статусом COMPLETED
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        Payment savedPayment = captor.getValue();
        assertEquals(10L, savedPayment.getOrderId());
        assertEquals(1L, savedPayment.getUserId());
        assertEquals(BigDecimal.valueOf(500), savedPayment.getAmount());
        assertEquals(PaymentStatus.COMPLETED, savedPayment.getStatus());
        assertNotNull(savedPayment.getTransactionId());
    }

    @Test
    void testProcessPayment_OtherException() {
        // Настраиваем баланс
        BalanceDTO balanceDTO = new BalanceDTO(4L, BigDecimal.valueOf(1000));
        when(balanceService.getBalance(4L)).thenReturn(balanceDTO);

        // Настраиваем исключение при списании средств
        when(balanceService.processPayment(4L, BigDecimal.valueOf(200)))
                .thenThrow(new RuntimeException("Some error"));

        boolean result = paymentService.processPayment(40L, 4L, BigDecimal.valueOf(200));
        assertFalse(result);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        Payment savedPayment = captor.getValue();
        assertEquals(PaymentStatus.FAILED, savedPayment.getStatus());
        assertEquals("Some error", savedPayment.getFailureReason());
    }

    @Test
    void testRefundPayment_Success() {
        // Готовим платеж со статусом COMPLETED
        Payment payment = Payment.builder()
                .id(500L)
                .orderId(50L)
                .userId(5L)
                .amount(BigDecimal.valueOf(300))
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findByOrderId(50L)).thenReturn(Optional.of(payment));

        doNothing().when(balanceService).refundPayment(5L, BigDecimal.valueOf(300));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.refundPayment(50L, 5L, BigDecimal.valueOf(300));

        // Проверяем, что статус стал REFUNDED
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertNotNull(payment.getRefundTimestamp());
        verify(balanceService).refundPayment(5L, BigDecimal.valueOf(300));
    }

    @Test
    void testRefundPayment_NotCompletedStatus() {
        // Платёж ещё не COMPLETED
        Payment payment = Payment.builder()
                .orderId(60L)
                .userId(6L)
                .amount(BigDecimal.valueOf(200))
                .status(PaymentStatus.FAILED)
                .build();
        when(paymentRepository.findByOrderId(60L)).thenReturn(Optional.of(payment));

        paymentService.refundPayment(60L, 6L, BigDecimal.valueOf(200));

        // Убедимся, что метод refundPayment у balanceService НЕ вызывался
        verify(balanceService, never()).refundPayment(anyLong(), any());
        // Статус не меняется
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void testRefundPayment_NotFound() {
        when(paymentRepository.findByOrderId(70L)).thenReturn(Optional.empty());
        Executable paymentRefund = () -> paymentService.refundPayment(70L, 7L, BigDecimal.valueOf(150));
        RuntimeException ex = assertThrows(RuntimeException.class, paymentRefund);
        assertTrue(ex.getMessage().contains("Payment not found for order: 70"));
    }

    @Test
    void testGetAllPayments() {
        List<Payment> payments = new ArrayList<>();
        payments.add(Payment.builder().id(1L).build());
        payments.add(Payment.builder().id(2L).build());

        when(paymentRepository.findAll()).thenReturn(payments);

        List<Payment> result = paymentService.getAllPayments();
        assertEquals(2, result.size());
        verify(paymentRepository).findAll();
    }

    @Test
    void testGetPaymentByOrderId_Found() {
        Payment payment = Payment.builder().id(3L).orderId(33L).build();
        when(paymentRepository.findByOrderId(33L)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentByOrderId(33L);
        assertNotNull(result);
        assertEquals(3L, result.getId());
        verify(paymentRepository).findByOrderId(33L);
    }

    @Test
    void testGetPaymentByOrderId_NotFound() {
        when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.getPaymentByOrderId(999L));
        assertTrue(ex.getMessage().contains("Payment not found for order: 999"));
    }
}
