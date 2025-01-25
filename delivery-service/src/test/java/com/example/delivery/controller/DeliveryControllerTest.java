package com.example.delivery.controller;

import com.example.delivery.exception.DeliveryNotFoundException;
import com.example.delivery.exception.GlobalExceptionHandler;
import com.example.delivery.model.Delivery;
import com.example.delivery.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private DeliveryController deliveryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(deliveryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getDeliveryByOrderId_ShouldReturnDelivery() throws Exception {
        Long orderId = 1L;
        Delivery delivery = new Delivery(1L, orderId, "123 Street", "TRACK123", null, null, null);

        when(deliveryService.getDeliveryByOrderId(orderId)).thenReturn(delivery);

        mockMvc.perform(get("/api/deliveries/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.trackingNumber").value("TRACK123"))
                .andExpect(jsonPath("$.address").value("123 Street"));
    }

    @Test
    void getDeliveryByOrderId_ShouldReturn404_WhenDeliveryNotFound() throws Exception {
        Long orderId = 1L;
        when(deliveryService.getDeliveryByOrderId(orderId))
                .thenThrow(new DeliveryNotFoundException("Delivery not found for order: " + orderId));

        mockMvc.perform(get("/api/deliveries/{orderId}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Delivery not found for order: " + orderId))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getAllDelivery_ShouldReturnList() throws Exception {
        List<Delivery> deliveries = List.of(
            new Delivery(1L, 2L,"123 Street", "TRACK123", null, null, null),
            new Delivery(2L, 3L,"456 Avenue", "TRACK456", null, null, null)
        );
        when(deliveryService.getAllDelivery()).thenReturn(deliveries);

        mockMvc.perform(get("/api/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].trackingNumber").value("TRACK123"))
                .andExpect(jsonPath("$[1].trackingNumber").value("TRACK456"));
    }

    @Test
    void getAllDelivery_ShouldReturnEmptyList() throws Exception {
        when(deliveryService.getAllDelivery()).thenReturn(List.of());

        mockMvc.perform(get("/api/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }
}
