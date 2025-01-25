package com.example.delivery.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DeliveryTest {

    @Test
    void testDeliveryBuilder() {
        LocalDateTime now = LocalDateTime.now();
        
        Delivery delivery = Delivery.builder()
                .id(1L)
                .orderId(2L)
                .address("ул. Пушкина, д. 10")
                .trackingNumber("TRACK123")
                .status(DeliveryStatus.IN_PROGRESS)
                .createdAt(now)
                .updatedAt(now)
                .build();
        
        assertEquals(1L, delivery.getId());
        assertEquals(2L, delivery.getOrderId());
        assertEquals("ул. Пушкина, д. 10", delivery.getAddress());
        assertEquals("TRACK123", delivery.getTrackingNumber());
        assertEquals(DeliveryStatus.IN_PROGRESS, delivery.getStatus());
        assertEquals(now, delivery.getCreatedAt());
        assertEquals(now, delivery.getUpdatedAt());
    }

    @Test
    void testDeliverySettersAndGetters() {
        Delivery delivery = new Delivery();
        LocalDateTime now = LocalDateTime.now();
        
        delivery.setId(1L);
        delivery.setOrderId(2L);
        delivery.setAddress("ул. Ленина, д. 15");
        delivery.setTrackingNumber("TRACK456");
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setCreatedAt(now);
        delivery.setUpdatedAt(now);
        
        assertEquals(1L, delivery.getId());
        assertEquals(2L, delivery.getOrderId());
        assertEquals("ул. Ленина, д. 15", delivery.getAddress());
        assertEquals("TRACK456", delivery.getTrackingNumber());
        assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
        assertEquals(now, delivery.getCreatedAt());
        assertEquals(now, delivery.getUpdatedAt());
    }

    @Test
    void testDeliveryConstructor() {
        Delivery delivery = new Delivery(1L, 2L, "ул. Гагарина, д. 20", "TRACK789", DeliveryStatus.FAILED, null, null);
        
        assertEquals(1L, delivery.getId());
        assertEquals(2L, delivery.getOrderId());
        assertEquals("ул. Гагарина, д. 20", delivery.getAddress());
        assertEquals("TRACK789", delivery.getTrackingNumber());
        assertEquals(DeliveryStatus.FAILED, delivery.getStatus());
    }

    @Test
    void testEqualsAndHashCode() {
        Delivery delivery1 = new Delivery(1L, 2L, "Address", "TRACK123", DeliveryStatus.IN_PROGRESS, null, null);
        Delivery delivery2 = new Delivery(1L, 2L, "Address", "TRACK123", DeliveryStatus.IN_PROGRESS, null, null);
        Delivery delivery3 = new Delivery(2L, 3L, "Other Address", "TRACK456", DeliveryStatus.DELIVERED, null, null);
        
        assertEquals(delivery1, delivery2);
        assertNotEquals(delivery1, delivery3);
        assertEquals(delivery1.hashCode(), delivery2.hashCode());
        assertNotEquals(delivery1.hashCode(), delivery3.hashCode());
    }
}
