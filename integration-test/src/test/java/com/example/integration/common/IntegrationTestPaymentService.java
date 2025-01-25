package com.example.integration.common;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Service
public class IntegrationTestPaymentService {
    private final IntegrationTestCommonConfig config;

    public IntegrationTestPaymentService(IntegrationTestCommonConfig config) {
        this.config = config;
    }

    /**
     * Process payment for the given order
     * @param orderId ID of the order to process
     * @param token Authentication token
     */
    public void processOrder(Long orderId, String token) {
        // Получаем информацию о заказе
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Map<String, Object>> orderResponse = config.getRestTemplate().exchange(
                config.getOrderUrl() + "/" + orderId,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );
        assertEquals(HttpStatus.OK, orderResponse.getStatusCode());

        Map<String, Object> order = orderResponse.getBody();
        assertNotNull(order);
        
        long userId = Long.parseLong(order.get("userId").toString());
        BigDecimal amount = new BigDecimal(order.get("totalAmount").toString());
        
        // Обрабатываем платеж
        ResponseEntity<Boolean> response = config.getRestTemplate().exchange(
            config.getPaymentUrl() + "/process?orderId=" + orderId + "&userId=" + userId + "&amount=" + amount,
            HttpMethod.POST,
            entity,
            Boolean.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Boolean.TRUE, response.getBody());
    }
}
