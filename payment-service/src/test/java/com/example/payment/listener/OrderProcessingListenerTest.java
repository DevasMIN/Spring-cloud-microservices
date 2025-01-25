package com.example.payment.listener;

import com.example.common.client.OrderServiceClient;
import com.example.common.dto.OrderDTO;
import com.example.common.enums.OrderStatus;
import com.example.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingListenerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private KafkaTemplate<String, OrderDTO> kafkaTemplate;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private OrderProcessingListener orderProcessingListener;

    @Test
    void handleNewOrder_ShouldProcessPaymentSuccessfully() {
        OrderDTO orderDTO = new OrderDTO(1L, 2L, 100.0, "123 Street", OrderStatus.REGISTERED);

        when(paymentService.processPayment(1L, 2L, new BigDecimal("100.0"))).thenReturn(true);

        orderProcessingListener.handleNewOrder(orderDTO);

        verify(orderServiceClient, times(1)).updateOrderStatus(1L, OrderStatus.PAID);
        verify(kafkaTemplate, times(1)).send(isNull(), anyString(), eq(orderDTO));
    }

    @Test
    void handleNewOrder_ShouldHandlePaymentFailure() {
        OrderDTO orderDTO = new OrderDTO(1L, 2L, 100.0, "123 Street", OrderStatus.REGISTERED);

        when(paymentService.processPayment(1L, 2L, new BigDecimal("100.0"))).thenReturn(false);

        orderProcessingListener.handleNewOrder(orderDTO);

        verify(orderServiceClient, times(1)).updateOrderStatus(1L, OrderStatus.PAYMENT_FAILED);
        verify(kafkaTemplate, times(1)).send(isNull(), anyString(), eq(orderDTO));
    }
}
