package com.example.order.controller;

import com.example.common.enums.OrderStatus;
import com.example.order.model.Order;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void updateOrderStatus_ShouldUpdateStatus() throws Exception {
        Long orderId = 1L;
        OrderStatus newStatus = OrderStatus.PAID;
        Order updatedOrder = new Order(orderId, 2L, new BigDecimal("100.0"), null, newStatus, Collections.emptyList(), Collections.emptyList());

        when(orderService.updateOrderStatus(eq(orderId), eq(newStatus), any())).thenReturn(updatedOrder);

        mockMvc.perform(patch("/api/orders/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"" + newStatus.name() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value(newStatus.toString()));
    }

    @Test
    void getOrder_ShouldReturnOrder() throws Exception {
        Long orderId = 1L;
        Order order = new Order(orderId, 2L, new BigDecimal("100.0"), null, OrderStatus.PAID, Collections.emptyList(), Collections.emptyList());

        when(orderService.getOrder(orderId)).thenReturn(order);

        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));
    }

    @Test
    void getAllOrders_ShouldReturnList() throws Exception {
        List<Order> orders = List.of(new Order(1L, 2L, new BigDecimal("100.0"), null, OrderStatus.PAID, Collections.emptyList(), Collections.emptyList()));
        when(orderService.getAllOrders()).thenReturn(orders);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

}
