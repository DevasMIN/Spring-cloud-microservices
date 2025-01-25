package com.example.payment.controller;

import com.example.payment.model.Payment;
import com.example.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    @Test
    void processPayment_ShouldReturnSuccess() throws Exception {
        Long orderId = 1L;
        Long userId = 2L;
        BigDecimal amount = new BigDecimal("100.0");

        when(paymentService.processPayment(orderId, userId, amount)).thenReturn(true);

        mockMvc.perform(post("/api/payments/process")
                        .param("orderId", orderId.toString())
                        .param("userId", userId.toString())
                        .param("amount", amount.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void getPaymentByOrderId_ShouldReturnPayment() throws Exception {
        Long orderId = 1L;
        Payment payment = new Payment(1L, orderId, 2L, new BigDecimal("100.0"), null, null, null, null, null);

        when(paymentService.getPaymentByOrderId(orderId)).thenReturn(payment);

        mockMvc.perform(get("/api/payments/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId));
    }
}
