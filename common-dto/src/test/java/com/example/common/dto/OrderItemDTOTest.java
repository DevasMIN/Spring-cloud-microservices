package com.example.common.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemDTOTest {

    @Test
    void testOrderItemDTOBuilder() {
        // given
        Long productId = 1L;
        Integer quantity = 2;
        BigDecimal price = new BigDecimal("50.00");

        // when
        OrderItemDTO orderItemDTO = OrderItemDTO.builder()
                .productId(productId)
                .quantity(quantity)
                .price(price)
                .build();

        // then
        assertThat(orderItemDTO.getProductId()).isEqualTo(productId);
        assertThat(orderItemDTO.getQuantity()).isEqualTo(quantity);
        assertThat(orderItemDTO.getPrice()).isEqualTo(price);
    }

    @Test
    void testOrderItemDTOConstructor() {
        // given
        Long productId = 1L;
        Integer quantity = 2;
        BigDecimal price = new BigDecimal("50.00");

        // when
        OrderItemDTO orderItemDTO = new OrderItemDTO(productId, quantity, price);

        // then
        assertThat(orderItemDTO.getProductId()).isEqualTo(productId);
        assertThat(orderItemDTO.getQuantity()).isEqualTo(quantity);
        assertThat(orderItemDTO.getPrice()).isEqualTo(price);
    }

    @Test
    void testOrderItemDTOSettersAndGetters() {
        // given
        OrderItemDTO orderItemDTO = new OrderItemDTO();
        Long productId = 1L;
        Integer quantity = 2;
        BigDecimal price = new BigDecimal("50.00");

        // when
        orderItemDTO.setProductId(productId);
        orderItemDTO.setQuantity(quantity);
        orderItemDTO.setPrice(price);

        // then
        assertThat(orderItemDTO.getProductId()).isEqualTo(productId);
        assertThat(orderItemDTO.getQuantity()).isEqualTo(quantity);
        assertThat(orderItemDTO.getPrice()).isEqualTo(price);
    }
}
