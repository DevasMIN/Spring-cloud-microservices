package com.example.inventory.controller;

import com.example.inventory.model.InventoryItem;
import com.example.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryItem> addInventoryItem(@RequestBody InventoryItem item) {
        return ResponseEntity.ok(inventoryService.addInventoryItem(item));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<InventoryItem> getInventoryItem(@PathVariable Long itemId) {
        InventoryItem item = inventoryService.getInventoryItem(itemId);
        return item != null ? ResponseEntity.ok(item) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAllInventoryItems() {
        return ResponseEntity.ok(inventoryService.getAllInventoryItems());
    }

    /**
     * Update an existing inventory item.
     * @param itemId The ID of the item to be updated.
     * @param item The updated item details.
     * @return The updated item.
     */
    @PutMapping("/{itemId}")
    public ResponseEntity<InventoryItem> updateInventoryItem(
            @PathVariable Long itemId,
            @RequestBody InventoryItem item) {
        return ResponseEntity.ok(inventoryService.updateInventoryItem(itemId, item));
    }


    @PutMapping
    public ResponseEntity<InventoryItem> updateInventoryItem(
            @RequestBody InventoryItem item) {
        return ResponseEntity.ok(inventoryService.updateInventoryItem(item.getId(), item));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteInventoryItem(@PathVariable Long itemId) {
        inventoryService.deleteInventoryItem(itemId);
        return ResponseEntity.ok().build();
    }
}
