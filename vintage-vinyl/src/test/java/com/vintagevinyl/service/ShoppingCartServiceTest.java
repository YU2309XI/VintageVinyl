package com.vintagevinyl.service;

import com.vintagevinyl.exception.RecordNotFoundException;
import com.vintagevinyl.model.*;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.repository.RecordRepository;
import com.vintagevinyl.repository.ShoppingCartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(ShoppingCartServiceTest.class);

    @Mock
    private ShoppingCartRepository shoppingCartRepository;

    @Mock
    private RecordRepository recordRepository;

    @InjectMocks
    private ShoppingCartService shoppingCartService;

    private User testUser;
    private Record testRecord;
    private ShoppingCart testCart;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        logger.info("Setting up test data");

        // Create test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        testRecord = new Record();
        testRecord.setId(1L);
        testRecord.setTitle("Test Record");
        testRecord.setPrice(new BigDecimal("29.99"));

        testCart = new ShoppingCart();
        testCart.setId(1L);
        testCart.setUser(testUser);

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setRecord(testRecord);
        testCartItem.setQuantity(1);
        testCartItem.setCart(testCart);
    }

    @Test
    void testAddItemToCart() {
        logger.info("Testing addItemToCart");

        // Set necessary mocks
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        // Execute test
        shoppingCartService.addItemToCart(testUser, 1L, 2);

        // Verify
        verify(shoppingCartRepository).save(any(ShoppingCart.class));

        logger.info("AddItemToCart test completed successfully");
    }

    @Test
    void testUpdateCartItemQuantity() {
        logger.info("Testing updateCartItemQuantity");

        // Set necessary mocks
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        // Add test item to the cart
        testCart.addItem(testCartItem);

        // Execute test
        shoppingCartService.updateCartItemQuantity(testUser, 1L, 3);

        // Verify
        assertEquals(3, testCartItem.getQuantity());
        verify(shoppingCartRepository).save(testCart);

        logger.info("UpdateCartItemQuantity test completed successfully");
    }

    @Test
    void testCalculateCartTotal() {
        logger.info("Testing calculateCartTotal");

        // Set necessary mocks
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        // Set test data
        testCartItem.setQuantity(2);
        testCart.addItem(testCartItem);

        // Execute test
        BigDecimal actualTotal = shoppingCartService.calculateCartTotal(testUser);

        // Verify
        BigDecimal expectedTotal = new BigDecimal("59.98"); // 29.99 * 2
        assertEquals(expectedTotal, actualTotal);
        logger.info("Expected total: {}, Actual total: {}", expectedTotal, actualTotal);
        logger.info("CalculateCartTotal test completed successfully");
    }

    @Test
    void testRemoveItemFromCart() {
        logger.info("Testing removeItemFromCart");

        // Set necessary mocks
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        // Add test item to the cart
        testCart.addItem(testCartItem);

        // Execute test
        shoppingCartService.removeItemFromCart(testUser, 1L);

        // Verify
        assertTrue(testCart.getItems().isEmpty());
        verify(shoppingCartRepository).save(testCart);

        logger.info("RemoveItemFromCart test completed successfully");
    }

    @Test
    void testInvalidQuantity() {
        logger.info("Testing invalid quantity handling");

        // Test negative quantity
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            shoppingCartService.updateCartItemQuantity(testUser, 1L, -1);
        });
        assertEquals("Quantity must be at least 1", exception.getMessage());

        logger.info("Invalid quantity test completed successfully");
    }

    @Test
    void testAddItemToCartWithNonExistentRecord() {
        logger.info("Testing addItemToCart with non-existent record");

        when(shoppingCartRepository.findByUser(any(User.class))).thenReturn(Optional.of(testCart));
        when(recordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecordNotFoundException.class, () -> {
            shoppingCartService.addItemToCart(testUser, 99L, 1);
        });

        logger.info("AddItemToCart with non-existent record test completed");
    }

    @Test
    void testAddItemWithZeroQuantity() {
        // Expect to throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            shoppingCartService.addItemToCart(testUser, 1L, 0);
        });
    }

    @Test
    void testUpdateQuantityWithMaxValue() {
        logger.info("Testing update quantity with maximum value");

        // Add test item to the cart
        testCart.addItem(testCartItem);

        // Test exceeding the maximum value
        int invalidQuantity = CartItem.MAX_QUANTITY + 1;
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            shoppingCartService.updateCartItemQuantity(testUser, 1L, invalidQuantity);
        });

        // Verify exception message
        assertEquals("Quantity cannot exceed " + CartItem.MAX_QUANTITY, exception.getMessage());

        logger.info("Update quantity with maximum value test completed");
    }

    @Test
    void testEmptyCartTotal() {
        logger.info("Testing empty cart total calculation");

        when(shoppingCartRepository.findByUser(any(User.class))).thenReturn(Optional.of(new ShoppingCart()));

        BigDecimal total = shoppingCartService.calculateCartTotal(testUser);
        assertEquals(BigDecimal.ZERO, total);

        logger.info("Empty cart total calculation test completed");
    }

    @Test
    void testAddMultipleItemsToCart() {
        logger.info("Testing adding multiple items to cart");

        // Create second product
        Record testRecord2 = new Record();
        testRecord2.setId(2L);
        testRecord2.setTitle("Test Record 2");
        testRecord2.setPrice(new BigDecimal("19.99"));

        when(shoppingCartRepository.findByUser(any(User.class))).thenReturn(Optional.of(testCart));
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(recordRepository.findById(2L)).thenReturn(Optional.of(testRecord2));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        // Add two different items
        shoppingCartService.addItemToCart(testUser, 1L, 2); // 29.99 * 2
        shoppingCartService.addItemToCart(testUser, 2L, 1); // 19.99 * 1

        BigDecimal expectedTotal = new BigDecimal("79.97"); // (29.99 * 2) + 19.99
        BigDecimal actualTotal = shoppingCartService.calculateCartTotal(testUser);
        assertEquals(expectedTotal, actualTotal);

        logger.info("Multiple items addition test completed");
    }

    @Test
    void testUpdateQuantityForNonExistentItem() {
        logger.info("Testing update quantity for non-existent item");

        when(shoppingCartRepository.findByUser(any(User.class))).thenReturn(Optional.of(testCart));

        assertThrows(RuntimeException.class, () -> {
            shoppingCartService.updateCartItemQuantity(testUser, 99L, 1);
        });

        logger.info("Update quantity for non-existent item test completed");
    }

    @Test
    void testClearCartWithItems() {
        logger.info("Testing clear cart with items");

        when(shoppingCartRepository.findByUser(any(User.class))).thenReturn(Optional.of(testCart));
        testCart.addItem(testCartItem);

        assertFalse(testCart.getItems().isEmpty());
        shoppingCartService.clearCart(testUser);
        assertTrue(testCart.getItems().isEmpty());

        logger.info("Clear cart test completed");
    }

    @Test
    void testGetCartItemCount() {
        logger.info("Testing get cart item count");

        when(shoppingCartRepository.findByUser(any(User.class))).thenReturn(Optional.of(testCart));

        testCartItem.setQuantity(3);
        testCart.addItem(testCartItem);

        int count = shoppingCartService.getCartItemCount(testUser);
        assertEquals(3, count);

        logger.info("Get cart item count test completed");
    }
}

