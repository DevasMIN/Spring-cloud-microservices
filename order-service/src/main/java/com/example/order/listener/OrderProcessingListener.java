package com.example.order.listener;

import com.example.common.dto.OrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderProcessingListener {
    @KafkaListener(topics = "${kafka.topics.delivery-result}")
    public void handleOrderRollback(OrderDTO order) {
        log.info("Received final request for order: {}", order);
    }
}
