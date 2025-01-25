package com.example.delivery.controller;

import com.example.delivery.model.Delivery;
import com.example.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@Tag(name = "Delivery API", description = "API for managing deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/{orderId}")
    @Operation(summary = "Get delivery status for an order")
    public ResponseEntity<Delivery> getDeliveryByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.getDeliveryByOrderId(orderId));
    }

    @GetMapping
    @Operation(summary = "Get all delivery status for an order")
    public ResponseEntity<List<Delivery>> getAllDelivery() {
        return ResponseEntity.ok(deliveryService.getAllDelivery());
    }


}
