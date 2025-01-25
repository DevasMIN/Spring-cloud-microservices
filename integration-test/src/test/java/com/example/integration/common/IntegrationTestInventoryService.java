package com.example.integration.common;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RequiredArgsConstructor
@Service
public class IntegrationTestInventoryService {
    private final IntegrationTestCommonConfig config;
    private static final String TEST_PREFIX = "test_";

    public Long createInventoryItem(String sku, BigDecimal price, int quantity, String token) {
        Map<String, Object> request = new HashMap<>();
        request.put("sku", sku);
        request.put("price", price);
        request.put("quantity", quantity);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<Map<String, Object>> response = config.getRestTemplate().exchange(
                config.getInventoryUrl(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return ((Number)response.getBody().get("id")).longValue();
    }

    public int getInventoryQuantity(Long itemId, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        ResponseEntity<Map<String, Object>> response = config.getRestTemplate().exchange(
                config.getInventoryUrl() + "/" + itemId,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<>() {
                }
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return ((Number)response.getBody().get("quantity")).intValue();
    }

    public void deleteTestItems(String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        // Get all items
        ResponseEntity<List<Map<String, Object>>> response = config.getRestTemplate().exchange(
            config.getInventoryUrl(),
            HttpMethod.GET,
            new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                }
        );

        if (response.getBody() != null) {
            response.getBody().stream()
                .filter(item -> ((String)item.get("sku")).startsWith(TEST_PREFIX))
                .forEach(item -> {
                    // Delete the item
                    config.getRestTemplate().exchange(
                        config.getInventoryUrl() + "/" + item.get("id"),
                        HttpMethod.DELETE,
                        new HttpEntity<>(headers),
                        Void.class
                    );
                });
        }
    }
}
