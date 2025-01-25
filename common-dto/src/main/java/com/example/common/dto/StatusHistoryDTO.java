package com.example.common.dto;

import com.example.common.enums.OrderStatus;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class StatusHistoryDTO {
    private OrderStatus status;
    private LocalDateTime timestamp;
    private String comment;
}
