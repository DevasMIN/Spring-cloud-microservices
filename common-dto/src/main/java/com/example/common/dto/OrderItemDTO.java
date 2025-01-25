package com.example.common.dto;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@Builder
public class OrderItemDTO {
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
}
