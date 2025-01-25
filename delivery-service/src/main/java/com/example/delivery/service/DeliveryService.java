package com.example.delivery.service;

import com.example.common.dto.OrderDTO;
import com.example.common.enums.OrderStatus;
import com.example.delivery.exception.DeliveryNotFoundException;
import com.example.delivery.model.Delivery;
import com.example.delivery.model.DeliveryStatus;
import com.example.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;
    private final Random random = new Random();


    @Value("${kafka.topics.delivery-result}")
    private String deliveryResultTopic;

    @Value("${app.delivery.success-rate:0.85}")
    private double deliverySuccessRate;

    public void processDelivery(OrderDTO orderDTO) {
        log.info("Processing delivery for order: {}", orderDTO.getId());
        try {
            Delivery delivery = createDelivery(orderDTO);

            // Имитация времени на доставку
            simulateDeliveryTime();

            // Симулируем процесс доставки с 85% успешных доставок
            if (random.nextDouble() <= deliverySuccessRate) {
                // Успешная доставка
                delivery.setStatus(DeliveryStatus.DELIVERED);
                deliveryRepository.save(delivery);

                orderDTO.setStatus(OrderStatus.DELIVERED);
                kafkaTemplate.send(deliveryResultTopic, orderDTO.getId().toString(), orderDTO);

                log.info("Delivery completed successfully for order: {}", orderDTO.getId());
            } else {
                // Неудачная доставка
                delivery.setStatus(DeliveryStatus.FAILED);
                deliveryRepository.save(delivery);

                orderDTO.setStatus(OrderStatus.DELIVERY_FAILED);
                kafkaTemplate.send(deliveryResultTopic, orderDTO.getId().toString(), orderDTO);

                log.warn("Delivery failed for order: {}", orderDTO.getId());
            }
        } catch (Exception e) {
            log.error("Error processing delivery for order: {}", orderDTO.getId(), e);
            orderDTO.setStatus(OrderStatus.DELIVERY_FAILED);
            kafkaTemplate.send(deliveryResultTopic, orderDTO.getId().toString(), orderDTO);
        }
    }


    public List<Delivery> getAllDelivery() {
        return deliveryRepository.findAll();
    }

    public Delivery getDeliveryByOrderId(Long orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DeliveryNotFoundException("Product not found: " + orderId));
    }

    public Delivery createDelivery(OrderDTO orderDTO) {
        log.info("Creating delivery for order: {}", orderDTO.getId());

        Delivery delivery = new Delivery();
        delivery.setOrderId(orderDTO.getId());
        delivery.setAddress(orderDTO.getDeliveryAddress());
        delivery.setStatus(DeliveryStatus.IN_PROGRESS);
        delivery.setTrackingNumber(String.valueOf(random.nextFloat()));
        delivery.setCreatedAt(LocalDateTime.now());

        return deliveryRepository.save(delivery);
    }


    private void simulateDeliveryTime() {
        try {
            Thread.sleep(7000); // 7 seconds for delivery simulation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during delivery", e);
        }
    }
}
