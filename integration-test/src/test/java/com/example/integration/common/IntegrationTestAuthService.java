package com.example.integration.common;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RequiredArgsConstructor
@Service
public class IntegrationTestAuthService {
    private final IntegrationTestCommonConfig config;
    private static final String TEST_PREFIX = "test_";

    public Map<String, String> createUser(String username, String password, String email) {
        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);
        request.put("email", email);

        ResponseEntity<Map<String, Object>> response = config.getRestTemplate().exchange(
                config.getAuthUrl() + "/register",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {
                }
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, String> result = new HashMap<>();
        result.put("userId", response.getBody().get("userId").toString());
        result.put("token", response.getBody().get("token").toString());
        return result;
    }

    public String loginAsAdmin() {
        Map<String, String> request = new HashMap<>();
        request.put("username", "defaultAdmin");
        request.put("password", "defaultPassword");

        ResponseEntity<Map<String, Object>> response = config.getRestTemplate().exchange(
                config.getAuthUrl() + "/login",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {
                }
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return (String) response.getBody().get("token");
    }

    public void deleteTestUsers(String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // Get all users
        ResponseEntity<List<Map<String, Object>>> response = config.getRestTemplate().exchange(
                config.getUsersUrl(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );

        if (response.getBody() != null) {
            response.getBody().stream()
                .filter(user -> ((String)user.get("username")).startsWith(TEST_PREFIX))
                .forEach(user -> {
                    long userId = ((Number)user.get("id")).longValue();
                    // Delete the user
                    config.getRestTemplate().exchange(
                        config.getUsersUrl() + "/" + userId,
                        HttpMethod.DELETE,
                        entity,
                        Void.class
                    );
                });
        }
    }
}
