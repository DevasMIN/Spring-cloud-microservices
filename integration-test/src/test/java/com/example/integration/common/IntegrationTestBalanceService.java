package com.example.integration.common;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Service
@RequiredArgsConstructor
public class IntegrationTestBalanceService {
    private final IntegrationTestCommonConfig config;

    public void addBalance(Long userId, String adminToken, BigDecimal amount) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        // First create balance with 0
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("userId", userId);
        createRequest.put("amount", BigDecimal.ZERO);
        
        HttpEntity<Map<String, Object>> createEntity = new HttpEntity<>(createRequest, headers);
        
        try {
            config.getRestTemplate().exchange(
                config.getBalanceUrl(), 
                HttpMethod.POST, 
                createEntity, 
                Map.class
            );
        } catch (Exception ignored) {
            // Balance might already exist
        }

        // Then update it with the actual amount
        ResponseEntity<Map<String, Integer>> response = config.getRestTemplate().exchange(
                config.getBalanceUrl() + "/" + userId + "?amount=" + amount,
                HttpMethod.PUT,
                new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<>() {
                }
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    public BigDecimal getUserBalance(Long userId, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<Map<String, Integer>> response = config.getRestTemplate().exchange(
                config.getBalanceUrl() + "/" + userId ,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<>() {
                }
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return new BigDecimal(String.valueOf(response.getBody().get("amount")));
    }

    public void deleteUserBalance(Long userId, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        
        try {
            config.getRestTemplate().exchange(
                config.getBalanceUrl() + "/" + userId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
            );
        } catch (HttpClientErrorException.NotFound ignored) {
            // Balance not found - that's ok
        } catch (Exception e) {
            // Log other errors
            System.err.println("Error deleting balance for user " + userId + ": " + e.getMessage());
            throw e;
        }
    }

    public void deleteTestBalances(String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // Get all balances
        ResponseEntity<List<Map<String, Long>>> response = config.getRestTemplate().exchange(
            config.getBalanceUrl(),
            HttpMethod.GET,
            entity,
                new ParameterizedTypeReference<>() {
                }
        );

        if (response.getBody() != null) {
            response.getBody().forEach(balance -> {
                try {
                    Long userId = balance.get("userId");
                    deleteUserBalance(userId, adminToken);
                } catch (Exception e) {
                    // Log errors but continue processing
                    System.err.println("Error deleting balance: " + e.getMessage());
                }
            });
        }
    }
}
