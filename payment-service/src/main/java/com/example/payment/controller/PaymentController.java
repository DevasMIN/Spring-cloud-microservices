package com.example.payment.controller;

import com.example.payment.model.Payment;
import com.example.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment API", description = "API for managing payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @Operation(summary = "Process payment for an order")
    public ResponseEntity<Boolean> processPayment(@RequestParam Long orderId, 
                                                @RequestParam Long userId, 
                                                @RequestParam BigDecimal amount) {
        boolean success = paymentService.processPayment(orderId, userId, amount);
        return ResponseEntity.ok(success);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get payment details for an order")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund payment for an order")
    public ResponseEntity<Void> refundPayment(@RequestParam Long orderId, 
                                            @RequestParam Long userId, 
                                            @RequestParam BigDecimal amount) {
        paymentService.refundPayment(orderId, userId, amount);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Получить все платежи")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }
}
