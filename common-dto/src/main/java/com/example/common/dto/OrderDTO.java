package com.example.common.dto;

import com.example.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
    private String deliveryAddress;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
    private List<StatusHistoryDTO> statusHistory;

    public OrderDTO(long l, long l1, double totalAmount, String deliveryAddress, OrderStatus orderStatus) {
        this.id = l;
        this.userId = l1;
        this.totalAmount = BigDecimal.valueOf(totalAmount);
        this.deliveryAddress = deliveryAddress;
        this.status = orderStatus;
    }
}
