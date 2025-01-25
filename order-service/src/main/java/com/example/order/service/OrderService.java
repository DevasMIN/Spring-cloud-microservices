package com.example.order.service;

import com.example.common.dto.OrderItemDTO;
import com.example.common.dto.StatusHistoryDTO;
import com.example.common.enums.OrderStatus;
import com.example.order.dto.OrderRequest;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.mapper.OrderMapper;
import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import com.example.order.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final OrderMapper orderMapper;

    @Value("${kafka.topics.order-created}")
    private String orderCreated;

    @Transactional
    public Order createOrder(OrderRequest orderRequest) {
        Long userId = getCurrentUserId();
        log.info("Creating new order for user: {}", userId);
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.REGISTERED);
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(orderRequest.getItems().stream()
                .map(item -> new OrderItemDTO(item.getProductId(), item.getQuantity(), item.getPrice()))
                .toList());
        order.setTotalAmount(calculateTotalAmount(order.getItems()));
        order.setStatusHistoryDTO(new ArrayList<>());
        order.getStatusHistoryDTO().add(new StatusHistoryDTO(OrderStatus.REGISTERED, LocalDateTime.now(), "Order created"));

        order = orderRepository.save(order);
        
        // Send to payment service for processing
        log.info("Attempting to send order {} to topic {}", order.getId(), orderCreated);
        try {
            kafkaTemplate.send(orderCreated, orderMapper.toDto(order)).get();
            log.info("Successfully sent order {} to topic {}", order.getId(), orderCreated);
        } catch (Exception e) {
            log.error("Failed to send order {} to topic {}. Error: {}", order.getId(), orderCreated, e.getMessage(), e);
        }
        log.debug("Order created successfully: {}", order.getId());
        
        return order;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status, String message) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        
        order.setStatus(status);
        if (order.getStatusHistoryDTO() == null) {
            order.setStatusHistoryDTO(new ArrayList<>());
        }
        order.getStatusHistoryDTO().add(new StatusHistoryDTO(status, LocalDateTime.now(), message));
        
        order = orderRepository.save(order);
        log.debug("Updating order status: {} -> {} ({})", orderId, status, message);
        
        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        log.info("Deleting order: {}", orderId);
        if (!orderRepository.existsById(orderId)) {
            throw new OrderNotFoundException("Order not found: " + orderId);
        }
        orderRepository.deleteById(orderId);
        log.debug("Order deleted successfully: {}", orderId);
    }

    public void sendTestMessage(String message) {
        log.info("Attempting to send test message to topic {}", orderCreated);
        try {
            kafkaTemplate.send(orderCreated, message).get();
            log.info("Successfully sent test message to topic {}", orderCreated);
        } catch (Exception e) {
            log.error("Failed to send test message to topic {}. Error: {}", orderCreated, e.getMessage(), e);
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }
        throw new IllegalStateException("No authenticated user found");
    }



    private BigDecimal calculateTotalAmount(List<OrderItemDTO> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
