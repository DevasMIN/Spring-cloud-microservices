package com.example.integration;

import com.example.integration.common.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullCycleTest {
    private static final String TEST_PREFIX = "test_";

    @Autowired
    private IntegrationTestAuthService authService;
    
    @Autowired
    private IntegrationTestBalanceService balanceService;
    
    @Autowired
    private IntegrationTestInventoryService inventoryService;
    
    @Autowired
    private IntegrationTestOrderService orderService;
    
    @Autowired
    private IntegrationTestPaymentService paymentService;

    private Long userId1;
    private Long userId2;
    private Long itemId1;
    private Long itemId2;
    private String token1;
    private String token2;
    private String adminToken;

    @BeforeEach
    void setup() {
        // Login as admin
        adminToken = authService.loginAsAdmin();

        // Clear previous test data
        orderService.deleteTestOrders(adminToken);
        balanceService.deleteTestBalances(adminToken);
        inventoryService.deleteTestItems(adminToken);
        authService.deleteTestUsers(adminToken);

        // Create users with roles
        Map<String, String> user1 = authService.createUser(TEST_PREFIX + "user1", "password1", "user1@test.com");
        Map<String, String> user2 = authService.createUser(TEST_PREFIX + "user2", "password2", "user2@test.com");
        
        userId1 = Long.parseLong(user1.get("userId"));
        userId2 = Long.parseLong(user2.get("userId"));
        token1 = user1.get("token");
        token2 = user2.get("token");

        // Add balance to users
        balanceService.addBalance(userId1, adminToken, new BigDecimal("1000"));
        balanceService.addBalance(userId2, adminToken, new BigDecimal("500"));

        // Create inventory items
        itemId1 = inventoryService.createInventoryItem(TEST_PREFIX + "KEYBOARD_001", new BigDecimal("100"), 10, adminToken);
        itemId2 = inventoryService.createInventoryItem(TEST_PREFIX + "MOUSE_001", new BigDecimal("50"), 20, adminToken);
    }

    @Test
    void successfulOrderTest() {
        // Create order
        Long orderId = orderService.createOrder(userId1, itemId1, 1, token1);
        
        // Process payment
        paymentService.processOrder(orderId, token1);
        
        // Wait for order to be completed
        await().atMost(60, TimeUnit.SECONDS)
               .until(() -> orderService.getOrderStatus(orderId, token1),
                        status -> status.equals("DELIVERED") || status.equals("DELIVERY_FAILED"));
        
        // Verify inventory was updated
        assertEquals(9, inventoryService.getInventoryQuantity(itemId1, token1));
        
        // Verify balance was deducted
        assertEquals(new BigDecimal("900"), balanceService.getUserBalance(userId1, token1));
    }


    @Test
    void inventoryFailureTest() {
        // Create item with limited quantity
        Long itemId = inventoryService.createInventoryItem(TEST_PREFIX + "LIMITED_001", new BigDecimal("100"), 1, adminToken);
        
        // First order should succeed
        Long orderId1 = orderService.createOrder(userId1, itemId, 1, token1);
        paymentService.processOrder(orderId1, token1);

        await()
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> orderService.getOrderStatus(orderId1, token1),
                        status -> status.equals("DELIVERED") || status.equals("DELIVERY_FAILED"));
        
        // Second order should fail due to insufficient inventory
        Long orderId2 = orderService.createOrder(userId2, itemId, 1, token2);
        paymentService.processOrder(orderId2, token2);
        
        await()
            .atMost(50, TimeUnit.SECONDS)
            .until(() -> "INVENTORY_FAILED".equals(orderService.getOrderStatus(orderId2, token2)));
        
        // Verify balance was not deducted for failed order
        assertEquals(new BigDecimal("500"), balanceService.getUserBalance(userId2, token2));
    }

    @Test
    void deliveryTest() {
        // Create order
        Long orderId = orderService.createOrder(userId1, itemId2, 1, token1);
        
        // Process payment
        paymentService.processOrder(orderId, token1);
        
        // Wait for order to be completed
        await().atMost(60, TimeUnit.SECONDS)
               .until(() -> orderService.getOrderStatus(orderId, token1),
                        status -> status.equals("DELIVERED") || status.equals("DELIVERY_FAILED"));
        
        // Verify inventory was updated
        assertEquals(19, inventoryService.getInventoryQuantity(itemId2, token1));
        
        // Verify balance was deducted
        assertEquals(new BigDecimal("950"), balanceService.getUserBalance(userId1, token1));
    }

    @Test
    void deliveryFailureRollbackTest() {
        // Create item and check initial quantity
        Long itemId = inventoryService.createInventoryItem(TEST_PREFIX + "TEST_001", new BigDecimal("100"), 5, adminToken);
        int initialQuantity = inventoryService.getInventoryQuantity(itemId, adminToken);
        
        // Check initial balance
        BigDecimal initialBalance = balanceService.getUserBalance(userId1, token1);
        
        // Create order
        Long orderId = orderService.createOrder(userId1, itemId, 1, token1);
        
        // Process payment
        paymentService.processOrder(orderId, token1);
        
        // Wait for delivery result
        await()
            .atMost(60, TimeUnit.SECONDS)
            .until(() -> {
                String status = orderService.getOrderStatus(orderId, token1);
                return "DELIVERED".equals(status) || "DELIVERY_FAILED".equals(status);
            });
        
        String finalStatus = orderService.getOrderStatus(orderId, token1);
        if ("DELIVERY_FAILED".equals(finalStatus)) {
            // Verify that inventory was restored
            int finalQuantity = inventoryService.getInventoryQuantity(itemId, adminToken);
            assertEquals(initialQuantity, finalQuantity, "Inventory quantity should be restored after delivery failure");
            
            // Verify that balance was returned
            BigDecimal finalBalance = balanceService.getUserBalance(userId1, token1);
            assertEquals(initialBalance, finalBalance, "Balance should be returned after delivery failure");
        }
    }
}
