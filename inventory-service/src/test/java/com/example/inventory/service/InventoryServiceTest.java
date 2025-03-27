package com.example.inventory.service;

import com.example.common.dto.OrderDTO;
import com.example.common.dto.OrderItemDTO;
import com.example.common.enums.OrderStatus;
import com.example.inventory.exception.ItemNotFoundException;
import com.example.inventory.model.InventoryItem;
import com.example.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Устанавливаем значения полей из @Value
        ReflectionTestUtils.setField(inventoryService, "inventoryReservedTopic", "inventory-reserved-topic");
        ReflectionTestUtils.setField(inventoryService, "inventoryFailedTopic", "inventory-failed-topic");
    }

    @Test
    void testProcessInventory_Success() {
        // Подготовка тестовых данных
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(1L);
        orderDTO.setItems(new ArrayList<>());

        OrderItemDTO item1 = new OrderItemDTO();
        item1.setProductId(100L);
        item1.setQuantity(2);
        orderDTO.getItems().add(item1);

        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setId(100L);
        inventoryItem.setQuantity(10);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventoryItem));

        // Выполняем тестируемый метод
        boolean result = inventoryService.processInventory(orderDTO);

        // Проверяем результат
        assertTrue(result);
        verify(inventoryRepository).save(argThat(inv -> inv.getId().equals(100L) && inv.getQuantity() == 8));
    }

    @Test
    void testProcessInventory_ProductNotFound() {
        // Создаём OrderDTO без InventoryItem в репозитории
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(2L);

        List<OrderItemDTO> items = new ArrayList<>();
        OrderItemDTO item1 = new OrderItemDTO();
        item1.setProductId(200L);
        item1.setQuantity(5);
        items.add(item1);

        orderDTO.setItems(items);

        when(inventoryRepository.findById(200L)).thenReturn(Optional.empty());

        boolean result = inventoryService.processInventory(orderDTO);
        assertFalse(result);
    }

    @Test
    void testProcessInventory_NotEnoughQuantity() {
        // Создаём OrderDTO с товаром, которого меньше, чем запрашивается
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(3L);

        List<OrderItemDTO> items = new ArrayList<>();
        OrderItemDTO item = new OrderItemDTO();
        item.setProductId(300L);
        item.setQuantity(10);
        items.add(item);

        orderDTO.setItems(items);

        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setId(300L);
        inventoryItem.setQuantity(5);

        when(inventoryRepository.findById(300L)).thenReturn(Optional.of(inventoryItem));

        boolean result = inventoryService.processInventory(orderDTO);

        assertFalse(result);
    }

    @Test
    void testRestoreInventory() {
        // Тестируем метод восстановления
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(10L);

        OrderItemDTO item1 = new OrderItemDTO();
        item1.setProductId(100L);
        item1.setQuantity(3);

        OrderItemDTO item2 = new OrderItemDTO();
        item2.setProductId(200L);
        item2.setQuantity(5);

        List<OrderItemDTO> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);

        orderDTO.setItems(items);

        InventoryItem inventoryItem1 = new InventoryItem();
        inventoryItem1.setId(100L);
        inventoryItem1.setQuantity(10);

        InventoryItem inventoryItem2 = new InventoryItem();
        inventoryItem2.setId(200L);
        inventoryItem2.setQuantity(5);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventoryItem1));
        when(inventoryRepository.findById(200L)).thenReturn(Optional.of(inventoryItem2));

        inventoryService.restoreInventory(orderDTO);

        // Убедимся, что количество восстановилось
        verify(inventoryRepository).save(argThat(inv -> inv.getId().equals(100L) && inv.getQuantity() == 13));
        verify(inventoryRepository).save(argThat(inv -> inv.getId().equals(200L) && inv.getQuantity() == 10));
    }

    @Test
    void testAddInventoryItem() {
        InventoryItem item = new InventoryItem();
        item.setId(123L);
        item.setSku("SKU-123");
        item.setQuantity(50);
        item.setPrice(BigDecimal.valueOf(999.0));

        when(inventoryRepository.save(item)).thenReturn(item);

        InventoryItem result = inventoryService.addInventoryItem(item);
        assertNotNull(result);
        assertEquals(123L, result.getId());
        assertEquals("SKU-123", result.getSku());
        assertEquals(50, result.getQuantity());
        assertEquals(BigDecimal.valueOf(999.0), result.getPrice());

        verify(inventoryRepository).save(item);
    }

    @Test
    void testGetInventoryItem() {
        InventoryItem item = new InventoryItem();
        item.setId(10L);

        when(inventoryRepository.findById(10L)).thenReturn(Optional.of(item));

        InventoryItem result = inventoryService.getInventoryItem(10L);
        assertNotNull(result);
        assertEquals(10L, result.getId());

        verify(inventoryRepository).findById(10L);
    }

    @Test
    void testGetInventoryItem_NotFound() {
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ItemNotFoundException.class, () -> inventoryService.getInventoryItem(999L));
    }

    @Test
    void testGetAllInventoryItems() {
        List<InventoryItem> items = new ArrayList<>();
        items.add(new InventoryItem());
        when(inventoryRepository.findAll()).thenReturn(items);

        List<InventoryItem> result = inventoryService.getAllInventoryItems();
        assertEquals(1, result.size());
        verify(inventoryRepository).findAll();
    }

    @Test
    void testUpdateInventory() {
        InventoryItem existing = new InventoryItem();
        existing.setId(1000L);
        existing.setQuantity(5);

        when(inventoryRepository.findById(1000L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItem updated = inventoryService.updateInventory(1000L, 10);
        assertEquals(1000L, updated.getId());
        assertEquals(10, updated.getQuantity());

        verify(inventoryRepository).findById(1000L);
        verify(inventoryRepository).save(argThat(inv -> inv.getQuantity() == 10));
    }

    @Test
    void testUpdateInventoryItem() {
        InventoryItem existingItem = new InventoryItem();
        existingItem.setId(1000L);
        existingItem.setSku("OLD-SKU");
        existingItem.setQuantity(5);
        existingItem.setPrice(BigDecimal.valueOf(500.0));

        InventoryItem newItem = new InventoryItem();
        newItem.setSku("NEW-SKU");
        newItem.setQuantity(10);
        newItem.setPrice(BigDecimal.valueOf(1000.0));

        when(inventoryRepository.findById(1000L)).thenReturn(Optional.of(existingItem));
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItem result = inventoryService.updateInventoryItem(1000L, newItem);
        assertEquals("NEW-SKU", result.getSku());
        assertEquals(10, result.getQuantity());
        assertEquals(BigDecimal.valueOf(1000.0), result.getPrice());

        verify(inventoryRepository).save(argThat(inv ->
                "NEW-SKU".equals(inv.getSku()) &&
                        inv.getQuantity() == 10 &&
                        Objects.equals(inv.getPrice(), BigDecimal.valueOf(1000.0))
        ));
    }

    @Test
    void testDeleteInventoryItem() {
        InventoryItem item = new InventoryItem();
        item.setId(77L);

        when(inventoryRepository.findById(77L)).thenReturn(Optional.of(item));

        inventoryService.deleteInventoryItem(77L);
        verify(inventoryRepository).delete(item);
    }
}
