package com.example.order.mapper;

import com.example.common.dto.OrderDTO;
import com.example.common.dto.OrderItemDTO;
import com.example.common.dto.StatusHistoryDTO;
import org.springframework.stereotype.Component;


@Component
public class OrderMapper {
    
    public OrderDTO toDto(com.example.order.model.Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream()
                        .map(this::toDto)
                        .toList())
                .statusHistory(order.getStatusHistoryDTO().stream()
                        .map(this::toDto)
                        .toList())
                .build();
    }
    
    private OrderItemDTO toDto(OrderItemDTO item) {
        return new OrderItemDTO(
                item.getProductId(),
                item.getQuantity(),
                item.getPrice()
        );
    }
    
    private StatusHistoryDTO toDto(StatusHistoryDTO history) {
        return new StatusHistoryDTO(
                history.getStatus(),
                history.getTimestamp(),
                history.getComment()
        );
    }
}
