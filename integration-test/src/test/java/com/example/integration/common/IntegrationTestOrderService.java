package com.example.integration.common;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Service
public class IntegrationTestOrderService {
    private final IntegrationTestCommonConfig config;

    public IntegrationTestOrderService(IntegrationTestCommonConfig config) {
        this.config = config;
    }

    public Long createOrder(Long userId, Long itemId, int quantity, String token) {
        System.out.println("Creating order for userId=" + userId + ", itemId=" + itemId + ", quantity=" + quantity);
        
        // Get item price from inventory
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<?> getEntity = new HttpEntity<>(headers);
        
        System.out.println("Getting item info from inventory...");
        ResponseEntity<Map<String, Object>> itemResponse = config.getRestTemplate().exchange(
                config.getInventoryUrl() + "/" + itemId,
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {
                }
        );
        assertEquals(HttpStatus.OK, itemResponse.getStatusCode());
        assertNotNull(itemResponse.getBody());
        System.out.println("Item info: " + itemResponse.getBody());
        BigDecimal price = new BigDecimal(itemResponse.getBody().get("price").toString());

        // Create order request
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("deliveryAddress", "Test Address, 123");
        
        Map<String, Object> item = new HashMap<>();
        item.put("productId", itemId);
        item.put("quantity", quantity);
        item.put("price", price);
        request.put("items", List.of(item));
        
        System.out.println("Creating order with request: " + request);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<Map<String, Object>> response = config.getRestTemplate().exchange(
                config.getOrderUrl(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Long orderId = Long.valueOf(response.getBody().get("id").toString());
        System.out.println("Order created with id: " + orderId);
        return orderId;
    }

    public String getOrderStatus(Long orderId, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        ResponseEntity<Map<String, Object>> response = config.getRestTemplate().exchange(
                config.getOrderUrl() + "/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<>() {
                }
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody().get("status").toString();
    }

    public void deleteTestOrders(String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        // Get all orders
        ResponseEntity<List<Map<String, Object>>> response = config.getRestTemplate().exchange(
            config.getOrderUrl(),
            HttpMethod.GET,
            new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                }
        );

        if (response.getBody() != null) {
            response.getBody().forEach(order -> {
                // Delete the order
                config.getRestTemplate().exchange(
                    config.getOrderUrl() + "/" + order.get("id"),
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    Void.class
                );
            });
        }
    }
}
