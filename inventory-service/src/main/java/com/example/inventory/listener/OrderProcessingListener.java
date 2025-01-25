package com.example.inventory.listener;

import com.example.common.client.OrderServiceClient;
import com.example.common.dto.OrderDTO;
import com.example.common.enums.OrderStatus;
import com.example.inventory.service.InventoryService;
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
    
    private final InventoryService inventoryService;
    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;
    private final OrderServiceClient orderServiceClient;

    @Value("${kafka.topics.inventory-reserved}")
    private String inventoryReservedTopic;

    @Value("${kafka.topics.inventory-failed}")
    private String inventoryFailedTopic;

    @KafkaListener(topics = "${kafka.topics.payment-success}")
    public void handleNewOrder(OrderDTO orderDTO) {
        log.info("Received paid order for inventory processing. Order details: id={}, userId={}, status={}", 
            orderDTO.getId(), orderDTO.getUserId(), orderDTO.getStatus());
        
        try {
            if (orderDTO.getStatus() == OrderStatus.PAID) {
                boolean success = inventoryService.processInventory(orderDTO);
                
                if (success) {
                    orderDTO.setStatus(OrderStatus.INVENTORY_DONE);
                    orderServiceClient.updateOrderStatus(
                        orderDTO.getId(),
                        OrderStatus.INVENTORY_DONE,
                "Inventory reserved successfully"
                    );
                    log.info("Inventory reserved successfully for order: {}, proceeding to delivery", orderDTO.getId());
                    kafkaTemplate.send(inventoryReservedTopic, orderDTO.getId().toString(), orderDTO);
                } else {
                    orderDTO.setStatus(OrderStatus.INVENTORY_FAILED);
                    orderServiceClient.updateOrderStatus(
                        orderDTO.getId(),
                        OrderStatus.INVENTORY_FAILED,
                        "Inventory reservation failed"
                    );
                    log.error("Inventory reservation failed for order: {} - insufficient stock", orderDTO.getId());
                    kafkaTemplate.send(inventoryFailedTopic, orderDTO.getId().toString(), orderDTO);
                }
            } else {
                log.warn("Order {} is not in PAID status. Current status: {}", orderDTO.getId(), orderDTO.getStatus());
            }
        } catch (Exception e) {
            String errorMessage = String.format("Error processing inventory for order: %s. Error: %s",
                    orderDTO.getId(),
                    e.getMessage());

            log.error(errorMessage);
            orderDTO.setStatus(OrderStatus.UNEXPECTED_FAILURE);
            orderServiceClient.updateOrderStatus(
                orderDTO.getId(),
                OrderStatus.UNEXPECTED_FAILURE,
                    errorMessage
            );
            kafkaTemplate.send(inventoryFailedTopic, orderDTO.getId().toString(), orderDTO);
        }
    }

    @KafkaListener(topics = "${kafka.topics.delivery-result}")
    public void handleOrderRollback(OrderDTO orderDTO) {
        log.info("Received rollback request for order: {}", orderDTO.getId());
        
        try {
            if (orderDTO.getStatus() == OrderStatus.DELIVERY_FAILED) {
                
                inventoryService.restoreInventory(orderDTO);
                log.info("Inventory restored for order: {}", orderDTO.getId());

            }
        } catch (Exception e) {
            log.error("Error processing inventory restore for order: {}", orderDTO.getId(), e);
            orderDTO.setStatus(OrderStatus.UNEXPECTED_FAILURE);
            kafkaTemplate.send(inventoryFailedTopic, orderDTO.getId().toString(), orderDTO);
        }
    }
}
