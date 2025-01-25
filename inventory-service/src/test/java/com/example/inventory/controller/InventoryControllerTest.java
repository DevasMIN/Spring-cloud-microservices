package com.example.inventory.controller;

import com.example.inventory.model.InventoryItem;
import com.example.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController).build();
    }

    @Test
    void getInventoryItem_ShouldReturnItem() throws Exception {
        Long itemId = 1L;
        InventoryItem item = new InventoryItem(itemId, "SKU123", 10, new BigDecimal("99.99"));

        when(inventoryService.getInventoryItem(itemId)).thenReturn(item);

        mockMvc.perform(get("/api/inventory/{itemId}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU123"));
    }

    @Test
    void getAllInventoryItems_ShouldReturnList() throws Exception {
        List<InventoryItem> items = List.of(new InventoryItem(1L, "SKU123", 10, new BigDecimal("99.99")));
        when(inventoryService.getAllInventoryItems()).thenReturn(items);

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }
}