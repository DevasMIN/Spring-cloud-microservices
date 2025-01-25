package com.example.inventory.listener;

import com.example.common.client.OrderServiceClient;
import com.example.common.dto.OrderDTO;
import com.example.common.enums.OrderStatus;
import com.example.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingListenerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private KafkaTemplate<String, OrderDTO> kafkaTemplate;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private OrderProcessingListener orderProcessingListener;

    @BeforeEach
    void setUp() {
        // Initialize @Value properties
        ReflectionTestUtils.setField(orderProcessingListener, "inventoryReservedTopic", "inventory-reserved");
        ReflectionTestUtils.setField(orderProcessingListener, "inventoryFailedTopic", "inventory-failed");
    }

    @Test
    void handleNewOrder_ShouldProcessInventorySuccessfully() {
        // Arrange
        OrderDTO orderDTO = new OrderDTO(1L, 2L, 100.0, "123 Street", OrderStatus.PAID);

        // Mock the inventory service to return true (success)
        when(inventoryService.processInventory(orderDTO)).thenReturn(true);

        // Act
        orderProcessingListener.handleNewOrder(orderDTO);

        // Assert
        verify(inventoryService, times(1)).processInventory(orderDTO);
        verify(orderServiceClient, times(1))
                .updateOrderStatus(orderDTO.getId(), OrderStatus.INVENTORY_DONE, "Inventory reserved successfully");
        verify(kafkaTemplate, times(1)).send(eq("inventory-reserved"), eq("1"), eq(orderDTO));
    }

    @Test
    void handleNewOrder_ShouldHandleFailure() {
        // Arrange
        OrderDTO orderDTO = new OrderDTO(1L, 2L, 100.0, "123 Street", OrderStatus.PAID);

        // Mock the inventory service to throw an exception
        when(inventoryService.processInventory(orderDTO)).thenThrow(new RuntimeException("Inventory error"));

        // Act
        orderProcessingListener.handleNewOrder(orderDTO);

        // Assert
        verify(orderServiceClient, times(1))
                .updateOrderStatus(orderDTO.getId(), OrderStatus.UNEXPECTED_FAILURE, "Error processing inventory for order: 1. Error: Inventory error");
        verify(kafkaTemplate, times(1)).send(eq("inventory-failed"), eq("1"), eq(orderDTO));
    }
}