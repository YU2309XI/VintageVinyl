package com.vintagevinyl.integration;

import com.vintagevinyl.model.*;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.service.OrderService;
import com.vintagevinyl.service.ShoppingCartService;
import com.vintagevinyl.service.UserService;
import com.vintagevinyl.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for shopping flow, including cart operations and order creation
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ShoppingFlowIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ShoppingCartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RecordRepository recordRepository;

    private User testUser;
    private Record testRecord;

    /**
     * Setup test data before each test
     */
    @BeforeEach
    void setUp() {
        // Create test user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        // 使用 addRole 方法而不是直接设置 roles
        user.addRole("USER");

        testUser = userService.save(user);  // 直接使用 save 而不是 registerNewUser

        // Create test record
        Record record = new Record();
        record.setTitle("Test Album");
        record.setArtist("Test Artist");
        record.setPrice(new BigDecimal("29.99"));
        record.setStock(10);
        record.setLowStockThreshold(5);
        record.setReleaseDate(LocalDate.now());
        testRecord = recordRepository.save(record);
    }

    /**
     * Test complete shopping flow
     */
    @Test
    void completeShoppingFlow() {
        // Add item to cart
        cartService.addItemToCart(testUser, testRecord.getId(), 2);

        // Verify cart state
        ShoppingCart cart = cartService.getCart(testUser);
        assertNotNull(cart, "Cart should not be null");
        assertEquals(1, cart.getItems().size(), "Cart should contain one item");
        assertEquals(2, cart.getItems().getFirst().getQuantity(), "Cart item quantity should be 2");

        // Verify cart total
        BigDecimal expectedTotal = testRecord.getPrice().multiply(new BigDecimal("2"));
        assertEquals(expectedTotal, cart.getTotal(), "Cart total should be price * quantity");

        // Create order
        String shippingAddress = "123 Test St, Test City";
        String paymentMethod = "Credit Card";
        Long orderId = orderService.createOrder(testUser, cart, shippingAddress, paymentMethod);

        // Verify order
        Order order = orderService.getOrderById(orderId);
        assertNotNull(order, "Order should not be null");
        assertEquals("PENDING", order.getStatus(), "Order status should be PENDING");
        assertEquals(expectedTotal, order.getTotal(), "Order total should match cart total");
        assertEquals(1, order.getItems().size(), "Order should contain one item");
        assertEquals(2, order.getItems().getFirst().getQuantity(), "Order item quantity should be 2");

        // Verify cart is cleared after order creation
        cart = cartService.getCart(testUser);
        assertTrue(cart.getItems().isEmpty(), "Cart should be empty after order creation");

        // Verify stock update
        Record updatedRecord = recordRepository.findById(testRecord.getId()).orElseThrow();
        assertEquals(8, updatedRecord.getStock(), "Stock should be reduced by ordered quantity");
    }

    /**
     * Test shopping flow with insufficient stock
     */
    @Test
    void shoppingFlowWithInsufficientStock() {
        // Set stock to 1
        testRecord.setStock(1);
        recordRepository.save(testRecord);

        // Attempt to add more items than available stock
        Exception exception = assertThrows(IllegalStateException.class, () -> cartService.addItemToCart(testUser, testRecord.getId(), 2), "Should throw IllegalStateException when stock is insufficient");

        assertTrue(exception.getMessage().contains("Insufficient stock"),
                "Exception message should indicate insufficient stock");

        // Verify cart remains empty
        ShoppingCart cart = cartService.getCart(testUser);
        assertTrue(cart.getItems().isEmpty(), "Cart should be empty after failed attempt");

        // Verify stock unchanged
        Record updatedRecord = recordRepository.findById(testRecord.getId()).orElseThrow();
        assertEquals(1, updatedRecord.getStock(), "Stock should remain unchanged");
    }

    /**
     * Test cart operations with multiple items
     */
    @Test
    void multipleCartOperations() {
        // Create second test record
        Record secondRecord = new Record();
        secondRecord.setTitle("Second Album");
        secondRecord.setArtist("Second Artist");
        secondRecord.setPrice(new BigDecimal("19.99"));
        secondRecord.setStock(5);
        secondRecord = recordRepository.save(secondRecord);

        // Add items to cart
        cartService.addItemToCart(testUser, testRecord.getId(), 2);
        cartService.addItemToCart(testUser, secondRecord.getId(), 1);

        // Verify cart state
        ShoppingCart cart = cartService.getCart(testUser);
        assertEquals(2, cart.getItems().size(), "Cart should contain two items");

        // Calculate expected total
        BigDecimal expectedTotal = testRecord.getPrice()
                .multiply(new BigDecimal("2"))
                .add(secondRecord.getPrice());
        assertEquals(expectedTotal, cart.getTotal(), "Cart total should be sum of all items");

        // Remove one item
        cartService.removeItemFromCart(testUser, testRecord.getId());

        // Verify cart after removal
        cart = cartService.getCart(testUser);
        assertEquals(1, cart.getItems().size(), "Cart should contain one item after removal");
        assertEquals(secondRecord.getPrice(), cart.getTotal(), "Cart total should be updated after removal");
    }
}