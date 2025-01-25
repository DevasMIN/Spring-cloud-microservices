package com.example.delivery.service;

import com.example.common.dto.OrderDTO;
import com.example.common.enums.OrderStatus;
import com.example.delivery.exception.DeliveryNotFoundException;
import com.example.delivery.model.Delivery;
import com.example.delivery.model.DeliveryStatus;
import com.example.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private KafkaTemplate<String, OrderDTO> kafkaTemplate;

    @InjectMocks
    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(deliveryService, "deliveryResultTopic", "delivery-result-topic");
        ReflectionTestUtils.setField(deliveryService, "deliverySuccessRate", 0.85);
        Random mockRandom = mock(Random.class);
        ReflectionTestUtils.setField(deliveryService, "random", mockRandom);
    }

    @Test
    void testProcessDelivery_Success() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(1L);
        when(((Random) Objects.requireNonNull(ReflectionTestUtils.getField(deliveryService, "random"))).nextDouble()).thenReturn(0.3);

        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        deliveryService.processDelivery(orderDTO);

        assertEquals(OrderStatus.DELIVERED, orderDTO.getStatus());
        verify(deliveryRepository, atLeastOnce()).save(any(Delivery.class));
        verify(kafkaTemplate).send("delivery-result-topic", "1", orderDTO);
    }

    @Test
    void testProcessDelivery_Failed() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(2L);
        when(((Random) Objects.requireNonNull(ReflectionTestUtils.getField(deliveryService, "random"))).nextDouble()).thenReturn(0.9);

        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        deliveryService.processDelivery(orderDTO);

        assertEquals(OrderStatus.DELIVERY_FAILED, orderDTO.getStatus());
        verify(kafkaTemplate).send("delivery-result-topic", "2", orderDTO);
    }

    @Test
    void testProcessDelivery_Exception() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(3L);
        when(((Random) Objects.requireNonNull(ReflectionTestUtils.getField(deliveryService, "random"))).nextDouble())
                .thenThrow(new RuntimeException("Some random error"));

        deliveryService.processDelivery(orderDTO);

        assertEquals(OrderStatus.DELIVERY_FAILED, orderDTO.getStatus());
        verify(kafkaTemplate).send("delivery-result-topic", "3", orderDTO);
    }

    @Test
    void testCreateDelivery() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(10L);
        orderDTO.setDeliveryAddress("Some Address");

        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> {
                    Delivery d = invocation.getArgument(0);
                    d.setId(100L);
                    return d;
                });

        Delivery result = deliveryService.createDelivery(orderDTO);
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(10L, result.getOrderId());
        assertEquals("Some Address", result.getAddress());
        assertEquals(DeliveryStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getTrackingNumber());
        assertNotNull(result.getCreatedAt());

        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    void testGetDeliveryByOrderId_Found() {
        Long orderId = 1L;
        Delivery delivery = new Delivery();
        delivery.setOrderId(orderId);
        
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));
        
        Delivery result = deliveryService.getDeliveryByOrderId(orderId);
        
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        verify(deliveryRepository).findByOrderId(orderId);
    }

    @Test
    void testGetDeliveryByOrderId_NotFound() {
        Long orderId = 1L;
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        
        assertThrows(DeliveryNotFoundException.class, () -> 
            deliveryService.getDeliveryByOrderId(orderId)
        );
        
        verify(deliveryRepository).findByOrderId(orderId);
    }

    @Test
    void testGetAllDelivery() {
        List<Delivery> deliveries = List.of(
            new Delivery(1L, 1L, "Address 1", "TRACK1", DeliveryStatus.IN_PROGRESS, null, null),
            new Delivery(2L, 2L, "Address 2", "TRACK2", DeliveryStatus.DELIVERED, null, null)
        );
        
        when(deliveryRepository.findAll()).thenReturn(deliveries);
        
        List<Delivery> result = deliveryService.getAllDelivery();
        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("TRACK1", result.get(0).getTrackingNumber());
        assertEquals("TRACK2", result.get(1).getTrackingNumber());
        verify(deliveryRepository).findAll();
    }

    @Test
    void testGetAllDelivery_EmptyList() {
        when(deliveryRepository.findAll()).thenReturn(new ArrayList<>());
        
        List<Delivery> result = deliveryService.getAllDelivery();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(deliveryRepository).findAll();
    }
}
