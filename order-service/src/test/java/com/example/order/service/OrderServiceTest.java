package com.example.order.service;

import com.example.common.dto.OrderDTO;
import com.example.common.dto.OrderItemDTO;
import com.example.common.enums.OrderStatus;
import com.example.order.dto.OrderItemRequest;
import com.example.order.dto.OrderRequest;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.mapper.OrderMapper;
import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import com.example.order.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String ORDER_CREATED_TOPIC = "order-created";
    
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private Authentication authentication;
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setUserId(2L);
        order.setTotalAmount(new BigDecimal("100.0"));
        order.setStatus(OrderStatus.PAID);
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        order.setStatusHistoryDTO(new ArrayList<>());
        
        // Устанавливаем значение топика через ReflectionTestUtils
        ReflectionTestUtils.setField(orderService, "orderCreated", ORDER_CREATED_TOPIC);
    }

    private void mockSecurityContext() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(2L);
        
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getOrder_ShouldReturnOrder_WhenExists() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Order foundOrder = orderService.getOrder(1L);

        assertThat(foundOrder).isNotNull();
        assertThat(foundOrder.getId()).isEqualTo(1L);
    }

    @Test
    void getOrder_ShouldThrowException_WhenNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(1L));
    }

    @Test
    void updateOrderStatus_ShouldUpdateOrderStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order updatedOrder = orderService.updateOrderStatus(1L, OrderStatus.INVENTORY_FAILED, "INVENTORY FAILED");

        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.INVENTORY_FAILED);
        assertThat(updatedOrder.getStatusHistoryDTO()).isNotEmpty();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldCreateAndSendOrder() {
        // Arrange
        mockSecurityContext();
        OrderRequest orderRequest = createValidOrderRequest();
        Order savedOrder = createSavedOrder();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(kafkaTemplate.send(eq(ORDER_CREATED_TOPIC), any(OrderDTO.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderDTO());

        // Act
        Order result = orderService.createOrder(orderRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.REGISTERED);
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("100.0"));
        assertThat(result.getItems()).isNotEmpty();
        verify(kafkaTemplate).send(eq(ORDER_CREATED_TOPIC), any(OrderDTO.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldHandleKafkaError() {
        // Arrange
        mockSecurityContext();
        OrderRequest orderRequest = createValidOrderRequest();
        Order savedOrder = createSavedOrder();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(kafkaTemplate.send(eq(ORDER_CREATED_TOPIC), any(OrderDTO.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka error")));
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderDTO());

        // Act
        Order result = orderService.createOrder(orderRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.REGISTERED);
        verify(kafkaTemplate).send(eq(ORDER_CREATED_TOPIC), any(OrderDTO.class));
    }

    @Test
    void getAllOrders_ShouldReturnAllOrders() {
        // Arrange
        Order order2 = new Order();
        order2.setId(2L);
        order2.setUserId(2L);
        order2.setTotalAmount(new BigDecimal("200.0"));
        order2.setStatus(OrderStatus.REGISTERED);
        order2.setCreatedAt(LocalDateTime.now());
        order2.setItems(new ArrayList<>());
        order2.setStatusHistoryDTO(new ArrayList<>());

        List<Order> orders = List.of(order, order2);
        when(orderRepository.findAll()).thenReturn(orders);

        // Act
        List<Order> result = orderService.getAllOrders();

        // Assert
        assertThat(result).hasSize(2).containsExactlyElementsOf(orders);
        verify(orderRepository).findAll();
    }

    @Test
    void sendTestMessage_ShouldSendMessageToKafka() {
        // Arrange
        String testMessage = "test message";
        when(kafkaTemplate.send(ORDER_CREATED_TOPIC, testMessage))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        orderService.sendTestMessage(testMessage);

        // Assert
        verify(kafkaTemplate).send(ORDER_CREATED_TOPIC, testMessage);
    }

    @Test
    void sendTestMessage_ShouldHandleKafkaError() {
        when(kafkaTemplate.send(ORDER_CREATED_TOPIC, "test message"))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka error")));

        orderService.sendTestMessage("test message");

        verify(kafkaTemplate).send(ORDER_CREATED_TOPIC, "test message");
    }

    @Test
    void getCurrentUserId_ShouldThrowException_WhenNoAuthentication() {
        SecurityContextHolder.clearContext();
        OrderRequest orderRequest = createValidOrderRequest();

        assertThrows(IllegalStateException.class, () -> orderService.createOrder(orderRequest));
    }

    @Test
    void getCurrentUserId_ShouldThrowException_WhenNotCustomUserDetails() {
        // Arrange
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("not a CustomUserDetails");
        
        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        OrderRequest orderRequest = createValidOrderRequest();

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> orderService.createOrder(orderRequest));
    }

    private OrderRequest createValidOrderRequest() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(new OrderItemRequest(1L, 2, new BigDecimal("50.0"))));
        orderRequest.setDeliveryAddress("Test Address");
        return orderRequest;
    }

    private Order createSavedOrder() {
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUserId(2L);
        savedOrder.setTotalAmount(new BigDecimal("100.0"));
        savedOrder.setStatus(OrderStatus.REGISTERED);
        savedOrder.setCreatedAt(LocalDateTime.now());
        savedOrder.setItems(List.of(new OrderItemDTO(1L, 2, new BigDecimal("50.0"))));
        savedOrder.setStatusHistoryDTO(new ArrayList<>());
        return savedOrder;
    }
}
