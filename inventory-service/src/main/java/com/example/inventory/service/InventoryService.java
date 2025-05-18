package com.example.inventory.service;

import com.example.common.dto.OrderDTO;
import com.example.common.dto.OrderItemDTO;
import com.example.inventory.exception.ItemNotFoundException;
import com.example.inventory.model.InventoryItem;
import com.example.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Value("${kafka.topics.inventory-reserved}")
    private String inventoryReservedTopic;

    @Value("${kafka.topics.inventory-failed}")
    private String inventoryFailedTopic;

    @Transactional
    public boolean processInventory(OrderDTO orderDTO) {
        log.info("Starting inventory processing for order: {}", orderDTO.getId());
        List<Long> missingProducts = new ArrayList<>();

        try {
            // Проверяем доступность всех товаров
            for (OrderItemDTO item : orderDTO.getItems()) {
                log.info("Checking availability for product {} in order {}", item.getProductId(), orderDTO.getId());
                InventoryItem inventoryItem = inventoryRepository.findById(item.getProductId())
                        .orElse(null);

                log.info("Current inventory state for product {}: quantity={}, required={}",
                        item.getProductId(),
                        inventoryItem != null ? inventoryItem.getQuantity() : 0,
                        item.getQuantity());

                if (inventoryItem == null || inventoryItem.getQuantity() < item.getQuantity()) {
                    missingProducts.add(item.getProductId());
                    log.warn("Insufficient inventory for product {} in order {}. Required: {}, Available: {}",
                            item.getProductId(),
                            orderDTO.getId(),
                            item.getQuantity(),
                            inventoryItem != null ? inventoryItem.getQuantity() : 0);
                }
            }

            if (!missingProducts.isEmpty()) {
                log.error("Order {} failed: Products not available: {}", orderDTO.getId(), missingProducts);
                return false;
            }

            // Обновляем количество товаров
            for (OrderItemDTO item : orderDTO.getItems()) {
                log.info("Updating inventory for product {} in order {}", item.getProductId(), orderDTO.getId());
                InventoryItem inventoryItem = inventoryRepository.findById(item.getProductId())
                        .orElseThrow(() -> new ItemNotFoundException("Product not found: " + item.getProductId()));

                int oldQuantity = inventoryItem.getQuantity();
                int newQuantity = oldQuantity - item.getQuantity();
                inventoryItem.setQuantity(newQuantity);
                
                log.info("About to save inventory item {} with new quantity {}", item.getProductId(), newQuantity);
                InventoryItem savedItem = inventoryRepository.save(inventoryItem);
                log.info("Saved inventory item {} with quantity {}", savedItem.getId(), savedItem.getQuantity());
                
                // Проверяем, что изменения сохранились
                InventoryItem verifyItem = inventoryRepository.findById(item.getProductId())
                        .orElseThrow(() -> new ItemNotFoundException("Product not found after save: " + item.getProductId()));
                log.info("Verified inventory item {} quantity: {}", verifyItem.getId(), verifyItem.getQuantity());
            }

            log.info("Starting inventory work emulation for order {}", orderDTO.getId());
            emulateInventoryWork();
            log.info("Inventory work completed for order {}", orderDTO.getId());

            log.info("Inventory successfully reserved for order: {}", orderDTO.getId());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to process inventory for order: {}. Error: {}", orderDTO.getId(), e.getMessage(), e);
            throw e;
        }
    }


    @Transactional
    public void restoreInventory(OrderDTO orderDTO) {
        log.info("Starting inventory restoration for order: {}", orderDTO.getId());
        
        try {
            for (OrderItemDTO item : orderDTO.getItems()) {
                log.info("Restoring inventory for product {} in order {}", item.getProductId(), orderDTO.getId());
                InventoryItem inventoryItem = inventoryRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ItemNotFoundException("Product not found: " + item.getProductId()));
                
                int oldQuantity = inventoryItem.getQuantity();
                int newQuantity = oldQuantity + item.getQuantity();
                inventoryItem.setQuantity(newQuantity);
                inventoryRepository.save(inventoryItem);
                
                log.info("Inventory restored for product {} in order {}: {} -> {} (change: {})",
                        item.getProductId(), orderDTO.getId(), oldQuantity, newQuantity, item.getQuantity());
            }
            
            log.info("Successfully restored inventory for order: {}", orderDTO.getId());
        } catch (Exception e) {
            log.error("Failed to restore inventory for order: {}. Error: {}", orderDTO.getId(), e.getMessage(), e);
            throw e;
        }
    }

    public InventoryItem addInventoryItem(InventoryItem item) {
        log.debug("Adding inventory item: {}", item);
        return inventoryRepository.save(item);
    }

    public InventoryItem getInventoryItem(Long itemId) {
        log.info("Fetching inventory item: {}", itemId);
        InventoryItem item = inventoryRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Product not found: " + itemId));
        log.info("Retrieved inventory item {}: quantity={}", itemId, item.getQuantity());
        return item;
    }

    public List<InventoryItem> getAllInventoryItems() {
        log.debug("Getting all inventory items");
        return inventoryRepository.findAll();
    }

    @Transactional
    public InventoryItem updateInventory(Long itemId, Integer quantity) {
        return inventoryRepository.findById(itemId)
            .map(item -> {
                item.setQuantity(quantity);
                return inventoryRepository.save(item);
            })
            .orElseThrow(() -> new ItemNotFoundException("Product not found: " + itemId));
    }

    public InventoryItem updateInventoryItem(Long itemId, InventoryItem item) {
        log.debug("Updating inventory item with itemId: {}", itemId);
        InventoryItem existingItem = inventoryRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found in test: " + itemId));

        existingItem.setSku(item.getSku());
        existingItem.setQuantity(item.getQuantity());
        existingItem.setPrice(item.getPrice());

        return inventoryRepository.save(existingItem);
    }

    public void deleteInventoryItem(Long itemId) {
        log.debug("Deleting inventory item with itemId: {}", itemId);
        InventoryItem item = getInventoryItem(itemId);
        inventoryRepository.delete(item);
    }

    public void emulateInventoryWork() {
        try {
            Thread.sleep(5000); // 5 секунд на комплектацию
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during order assembly", e);
        }
    }
}
