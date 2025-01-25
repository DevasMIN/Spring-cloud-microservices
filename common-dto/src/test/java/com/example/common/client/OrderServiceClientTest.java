package com.example.common.client;

import com.example.common.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderServiceClient orderServiceClient;

    private final String orderServiceUrl = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderServiceClient, "orderServiceUrl", orderServiceUrl);

        // Устанавливаем значение URL сервиса заказов
        Mockito.lenient().when(restTemplate.exchange(
                any(String.class),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.OK));
    }

    @Test
    void testUpdateOrderStatus_Success() {
        Long orderId = 1L;
        OrderStatus status = OrderStatus.COMPLETED;
        String message = "Order status updated";

        orderServiceClient.updateOrderStatus(orderId, status, message);

        String expectedUrl = orderServiceUrl + "/api/orders/" + orderId;
        verify(restTemplate, times(1)).exchange(
                eq(expectedUrl),
                eq(HttpMethod.PATCH),
                argThat(entity -> {
                    Object body = entity.getBody();
                    if (body instanceof Map<?, ?> bodyMap) {
                        return status.equals(bodyMap.get("status")) && message.equals(bodyMap.get("comment"));
                    }
                    return false;
                }),
                eq(Void.class)
        );
    }

    @Test
    void testUpdateOrderStatus_Failure() {
        Long orderId = 2L;
        OrderStatus status = OrderStatus.UNEXPECTED_FAILURE;
        String message = "Some error happened";

        String expectedUrl = orderServiceUrl + "/api/orders/" + orderId;

        // Настраиваем restTemplate на выброс исключения
        doThrow(new RuntimeException("Service unavailable"))
                .when(restTemplate)
                .exchange(eq(expectedUrl), eq(HttpMethod.PATCH), any(HttpEntity.class), eq(Void.class));

        // Ожидаем выброс исключения
        assertThrows(RuntimeException.class, () -> orderServiceClient.updateOrderStatus(orderId, status, message));

        // Проверяем, что exchange был вызван с правильными аргументами
        verify(restTemplate, times(1)).exchange(
                eq(expectedUrl),
                eq(HttpMethod.PATCH),
                argThat(entity -> {
                    Object body = entity.getBody();
                    if (body instanceof Map<?, ?> bodyMap ) {
                        return status.equals(bodyMap.get("status")) && message.equals(bodyMap.get("comment"));
                    }
                    return false;
                }),
                eq(Void.class)
        );
    }
}
