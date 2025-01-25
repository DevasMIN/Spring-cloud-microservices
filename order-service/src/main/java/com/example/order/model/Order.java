package com.example.order.model;

import com.example.common.dto.OrderItemDTO;
import com.example.common.dto.StatusHistoryDTO;
import com.example.common.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @ElementCollection
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItemDTO> items;
    
    @ElementCollection
    @CollectionTable(name = "order_status_history", joinColumns = @JoinColumn(name = "order_id"))
    private List<StatusHistoryDTO> statusHistoryDTO;
}
