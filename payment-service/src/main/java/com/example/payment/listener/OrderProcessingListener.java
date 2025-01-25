package com.example.payment.listener;

import com.example.common.client.OrderServiceClient;
import com.example.common.dto.OrderDTO;
import com.example.common.enums.OrderStatus;
import com.example.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderProcessingListener {
    
    private final PaymentService paymentService;
    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;
    private final OrderServiceClient orderServiceClient;

    @Value("${kafka.topics.payment-success}")
    private String paymentSuccess;

    @Value("${kafka.topics.payment-failed}")
    private String paymentFailed;

    @KafkaListener(topics = "${kafka.topics.order-created}")
    public void handleNewOrder(OrderDTO orderDTO) {
        log.info("Received new order for processing. Order details: id={}, userId={}, totalAmount={}, status={}", 
            orderDTO.getId(), orderDTO.getUserId(), orderDTO.getTotalAmount(), orderDTO.getStatus());
        
        try {
            if (orderDTO.getStatus() == OrderStatus.REGISTERED) {
                log.info("Processing payment for order {} with amount {}", orderDTO.getId(), orderDTO.getTotalAmount());
                boolean success = paymentService.processPayment(
                    orderDTO.getId(),
                    orderDTO.getUserId(),
                    orderDTO.getTotalAmount()
                );
                
                if (success) {
                    orderDTO.setStatus(OrderStatus.PAID);
                    orderServiceClient.updateOrderStatus(
                        orderDTO.getId(),
                        OrderStatus.PAID
                    );
                    kafkaTemplate.send(paymentSuccess, orderDTO.getId().toString(), orderDTO);
                    log.info("Payment successful for order: {}, proceeding to inventory", orderDTO.getId());
                } else {
                    orderDTO.setStatus(OrderStatus.PAYMENT_FAILED);
                    orderServiceClient.updateOrderStatus(
                        orderDTO.getId(),
                        OrderStatus.PAYMENT_FAILED
                    );
                    log.error("Payment failed for order: {} - insufficient funds", orderDTO.getId());
                    kafkaTemplate.send(paymentFailed, orderDTO.getId().toString(), orderDTO);
                }
            } else {
                log.warn("Order {} is not in REGISTERED status. Current status: {}", orderDTO.getId(), orderDTO.getStatus());
            }
        } catch (Exception e) {
            log.error("Error processing payment for order: {}. Error: {}", orderDTO.getId(), e.getMessage(), e);
            orderDTO.setStatus(OrderStatus.UNEXPECTED_FAILURE);
            orderServiceClient.updateOrderStatus(
                orderDTO.getId(),
                OrderStatus.UNEXPECTED_FAILURE
            );
            kafkaTemplate.send(paymentFailed, orderDTO.getId().toString(), orderDTO);
        }
    }

    @KafkaListener(topics = {"${kafka.topics.inventory-failed}", "${kafka.topics.delivery-result}"})
    public void handleOrderRollback(OrderDTO orderDTO) {
        log.info("Received rollback request for order: {}", orderDTO);
        
        try {
            if (orderDTO.getStatus() == OrderStatus.PAID ||
                orderDTO.getStatus() == OrderStatus.INVENTORY_FAILED ||
                orderDTO.getStatus() == OrderStatus.DELIVERY_FAILED) {
                
                paymentService.refundPayment(
                    orderDTO.getId(),
                    orderDTO.getUserId(),
                    orderDTO.getTotalAmount()
                );

                log.info("Payment refunded for order: {}", orderDTO.getId());
            }
        } catch (Exception e) {
            log.error("Error processing refund for order: {}", orderDTO.getId(), e);
            orderDTO.setStatus(OrderStatus.UNEXPECTED_FAILURE);
            kafkaTemplate.send(paymentFailed, orderDTO.getId().toString(), orderDTO);
        }
    }
}
