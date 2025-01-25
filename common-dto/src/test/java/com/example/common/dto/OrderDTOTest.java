package com.example.common.dto;

import com.example.common.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDTOTest {

    @Test
    void testOrderDTOBuilder() {
        // given
        Long id = 1L;
        Long userId = 2L;
        BigDecimal totalAmount = new BigDecimal("100.00");
        OrderStatus status = OrderStatus.REGISTERED;
        LocalDateTime createdAt = LocalDateTime.now();
        List<OrderItemDTO> items = new ArrayList<>();
        items.add(new OrderItemDTO(1L, 2, new BigDecimal("50.00")));
        String deliveryAddress = "Test Address";

        // when
        OrderDTO orderDTO = OrderDTO.builder()
                .id(id)
                .userId(userId)
                .totalAmount(totalAmount)
                .status(status)
                .createdAt(createdAt)
                .items(items)
                .deliveryAddress(deliveryAddress)
                .build();

        // then
        assertThat(orderDTO.getId()).isEqualTo(id);
        assertThat(orderDTO.getUserId()).isEqualTo(userId);
        assertThat(orderDTO.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(orderDTO.getStatus()).isEqualTo(status);
        assertThat(orderDTO.getCreatedAt()).isEqualTo(createdAt);
        assertThat(orderDTO.getItems()).hasSize(1);
        assertThat(orderDTO.getDeliveryAddress()).isEqualTo(deliveryAddress);
    }

    @Test
    void testOrderDTOSettersAndGetters() {
        // given
        OrderDTO orderDTO = new OrderDTO();
        Long id = 1L;
        Long userId = 2L;
        BigDecimal totalAmount = new BigDecimal("100.00");
        OrderStatus status = OrderStatus.REGISTERED;
        LocalDateTime createdAt = LocalDateTime.now();
        List<OrderItemDTO> items = new ArrayList<>();
        String deliveryAddress = "Test Address";

        // when
        orderDTO.setId(id);
        orderDTO.setUserId(userId);
        orderDTO.setTotalAmount(totalAmount);
        orderDTO.setStatus(status);
        orderDTO.setCreatedAt(createdAt);
        orderDTO.setItems(items);
        orderDTO.setDeliveryAddress(deliveryAddress);

        // then
        assertThat(orderDTO.getId()).isEqualTo(id);
        assertThat(orderDTO.getUserId()).isEqualTo(userId);
        assertThat(orderDTO.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(orderDTO.getStatus()).isEqualTo(status);
        assertThat(orderDTO.getCreatedAt()).isEqualTo(createdAt);
        assertThat(orderDTO.getItems()).isEmpty();
        assertThat(orderDTO.getDeliveryAddress()).isEqualTo(deliveryAddress);
    }
}
