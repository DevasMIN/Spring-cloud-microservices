package com.example.common.client;

import com.example.common.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.order-service.url}")
    private String orderServiceUrl;

    public void updateOrderStatus(Long orderId, OrderStatus status) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(orderServiceUrl)
                    .path("/api/orders/{orderId}")
                    .buildAndExpand(orderId)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<OrderStatus> entity = new HttpEntity<>(status, headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    Void.class
            );
            log.info("Successfully updated order status: orderId={}, status={}", orderId, status);
        } catch (Exception e) {
            log.error("Failed to update order status: orderId={}, status={}", orderId, status, e);
            throw new RuntimeException("Failed to update order status", e);
        }
    }
}
