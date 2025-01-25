package com.example.delivery.listener;

import com.example.common.client.OrderServiceClient;
import com.example.common.dto.OrderDTO;
import com.example.common.enums.OrderStatus;
import com.example.delivery.service.DeliveryService;
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

    private final DeliveryService deliveryService;
    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;
    private final OrderServiceClient orderServiceClient;

    @Value("${kafka.topics.delivery-result}")
    private String deliveryResult;

    @KafkaListener(topics = "${kafka.topics.inventory-reserved}")
    public void handleNewOrder(OrderDTO orderDTO) {
        log.info("Received new order for processing. Order details: id={}, userId={}, totalAmount={}, status={}",
                orderDTO.getId(), orderDTO.getUserId(), orderDTO.getTotalAmount(), orderDTO.getStatus());
        
        if (orderDTO.getStatus() != OrderStatus.INVENTORY_DONE) {
            log.warn("Unexpected order status: {}. Expected: INVENTORY_DONE", orderDTO.getStatus());
            return;
        }

        try {
            deliveryService.processDelivery(orderDTO);

            orderServiceClient.updateOrderStatus(
                orderDTO.getId(),
                orderDTO.getStatus(),
                    "Delivery process completed for order"
            );
            log.info("Delivery process completed for order: {}, status: {}", 
                    orderDTO.getId(), orderDTO.getStatus());
        } catch (Exception e) {
            log.error("Failed to process delivery for order: {}", orderDTO.getId(), e);
            orderDTO.setStatus(OrderStatus.DELIVERY_FAILED);
            orderServiceClient.updateOrderStatus(
                orderDTO.getId(),
                OrderStatus.DELIVERY_FAILED,
                    "Failed to process delivery for order"
            );
            kafkaTemplate.send(deliveryResult, orderDTO.getId().toString(), orderDTO);
        }
    }
}
