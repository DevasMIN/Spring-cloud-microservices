package com.example.delivery.listener;

import com.example.common.client.OrderServiceClient;
import com.example.common.dto.OrderDTO;
import com.example.common.enums.OrderStatus;
import com.example.delivery.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingListenerTest {

    @Mock
    private DeliveryService deliveryService;

    @Mock
    private KafkaTemplate<String, OrderDTO> kafkaTemplate;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private OrderProcessingListener orderProcessingListener;

    @Test
    void handleNewOrder_ShouldProcessDeliverySuccessfully() {
        OrderDTO orderDTO = new OrderDTO(1L, 2L, 100.0, "123 Street", OrderStatus.INVENTORY_DONE);

        doAnswer(invocation -> {
            OrderDTO arg = invocation.getArgument(0);
            arg.setStatus(OrderStatus.DELIVERED); // Явно меняем статус внутри mock
            return null;
        }).when(deliveryService).processDelivery(orderDTO);

        orderProcessingListener.handleNewOrder(orderDTO);

        verify(deliveryService, times(1)).processDelivery(orderDTO);
        verify(orderServiceClient, times(1))
                .updateOrderStatus(orderDTO.getId(), OrderStatus.DELIVERED, "Delivery process completed for order");
    }

    @Test
    void handleNewOrder_ShouldHandleFailure() {
        OrderDTO orderDTO = new OrderDTO(1L, 2L,100.0, "123 Street",  OrderStatus.INVENTORY_DONE);

        doThrow(new RuntimeException("Delivery error")).when(deliveryService).processDelivery(orderDTO);
        orderProcessingListener.handleNewOrder(orderDTO);

        verify(orderServiceClient, times(1))
                .updateOrderStatus(orderDTO.getId(), OrderStatus.DELIVERY_FAILED, "Failed to process delivery for order");
        verify(kafkaTemplate, times(1)).send(isNull(), anyString(), any(OrderDTO.class));
    }
}
