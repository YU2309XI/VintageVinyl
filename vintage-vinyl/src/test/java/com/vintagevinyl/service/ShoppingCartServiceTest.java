package com.vintagevinyl.service;

import com.vintagevinyl.exception.RecordNotFoundException;
import com.vintagevinyl.model.*;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.repository.ShoppingCartRepository;
import com.vintagevinyl.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceTest {

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
        // Initialize test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        // Initialize test record with sufficient stock
        testRecord = new Record();
        testRecord.setId(1L);
        testRecord.setTitle("Test Record");
        testRecord.setPrice(new BigDecimal("29.99"));
        testRecord.setStock(10); // Adding sufficient stock for tests

        // Initialize test cart item
        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setRecord(testRecord);
        testCartItem.setQuantity(1);

        // Initialize test shopping cart
        testCart = new ShoppingCart();
        testCart.setId(1L);
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>());
        testCartItem.setCart(testCart);
        testCart.addItem(testCartItem);
    }

    /**
     * Test adding a new item to the cart
     */
    @Test
    @DisplayName("Should add new item to cart successfully")
    void addItemToCart_NewItem_Success() {
        Record newRecord = new Record();
        newRecord.setId(2L);
        newRecord.setTitle("Test Record 2");
        newRecord.setPrice(new BigDecimal("19.99"));
        newRecord.setStock(5);

        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(recordRepository.findById(2L)).thenReturn(Optional.of(newRecord));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        shoppingCartService.addItemToCart(testUser, 2L, 1);

        verify(shoppingCartRepository).save(any(ShoppingCart.class));
    }

    /**
     * Test updating the quantity of an existing item in the cart
     */
    @Test
    @DisplayName("Should update existing item quantity in cart")
    void addItemToCart_ExistingItem_UpdatesQuantity() {
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        shoppingCartService.addItemToCart(testUser, 1L, 2);

        assertEquals(2, testCartItem.getQuantity());
        verify(shoppingCartRepository).save(testCart);
    }

    /**
     * Test exception when adding more items than available stock
     */
    @Test
    @DisplayName("Should throw exception when insufficient stock")
    void addItemToCart_InsufficientStock_ThrowsException() {
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        assertThrows(IllegalStateException.class,
                () -> shoppingCartService.addItemToCart(testUser, 1L, 11));
        verify(shoppingCartRepository, never()).save(any(ShoppingCart.class));
    }

    /**
     * Test exception when adding an item with invalid quantity
     */
    @Test
    @DisplayName("Should throw exception when quantity is invalid")
    void addItemToCart_InvalidQuantity_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> shoppingCartService.addItemToCart(testUser, 1L, 0));
        assertThrows(IllegalArgumentException.class,
                () -> shoppingCartService.addItemToCart(testUser, 1L, CartItem.MAX_QUANTITY + 1));
    }

    /**
     * Test exception when the record to be added is not found
     */
    @Test
    @DisplayName("Should throw exception when record not found")
    void addItemToCart_RecordNotFound_ThrowsException() {
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(recordRepository.findById(999L)).thenReturn(Optional.empty());

        RecordNotFoundException exception = assertThrows(RecordNotFoundException.class,
                () -> shoppingCartService.addItemToCart(testUser, 999L, 1));
        assertEquals("Record not found with id: 999", exception.getMessage());

        verify(recordRepository).findById(999L);
        verify(shoppingCartRepository, never()).save(any(ShoppingCart.class));
    }

    /**
     * Test getting or creating a shopping cart for the user
     */
    @Test
    @DisplayName("Should get or create cart successfully")
    void getOrCreateCart_Success() {
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        ShoppingCart result = shoppingCartService.getOrCreateCart(testUser);

        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        verify(shoppingCartRepository).save(any(ShoppingCart.class));
    }

    /**
     * Test removing an item from the cart
     */
    @Test
    @DisplayName("Should remove item from cart successfully")
    void removeItemFromCart_Success() {
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        shoppingCartService.removeItemFromCart(testUser, 1L);

        verify(shoppingCartRepository).save(testCart);
        assertTrue(testCart.getItems().stream().noneMatch(item -> item.getRecord().getId().equals(1L)));
    }

    /**
     * Test clearing the shopping cart
     */
    @Test
    @DisplayName("Should clear cart successfully")
    void clearCart_Success() {
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        shoppingCartService.clearCart(testUser);

        assertTrue(testCart.getItems().isEmpty());
        verify(shoppingCartRepository).save(testCart);
    }

    /**
     * Test calculating the total of the shopping cart
     */
    @Test
    @DisplayName("Should calculate cart total correctly")
    void calculateCartTotal_Success() {
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        BigDecimal total = shoppingCartService.calculateCartTotal(testUser);

        assertEquals(new BigDecimal("29.99"), total);
    }

    /**
     * Test updating the quantity of a cart item
     */
    @Test
    @DisplayName("Should update cart item quantity successfully")
    void updateCartItemQuantity_Success() {
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        shoppingCartService.updateCartItemQuantity(testUser, 1L, 2);

        assertEquals(2, testCartItem.getQuantity());
        verify(shoppingCartRepository).save(testCart);
    }

    /**
     * Test exception when updating a non-existent cart item
     */
    @Test
    @DisplayName("Should throw exception when updating non-existent cart item")
    void updateCartItemQuantity_ItemNotFound_ThrowsException() {
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        assertThrows(RuntimeException.class,
                () -> shoppingCartService.updateCartItemQuantity(testUser, 999L, 2));
    }

    /**
     * Test validating items in the cart
     */
    @Test
    @DisplayName("Should validate cart items successfully")
    void validateCartItems_Success() {
        CartItem invalidItem = new CartItem();
        invalidItem.setQuantity(1);
        invalidItem.setRecord(null);
        testCart.addItem(invalidItem);

        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        shoppingCartService.validateCartItems(testUser);

        assertFalse(testCart.getItems().contains(invalidItem));
        verify(shoppingCartRepository).save(testCart);
    }

    /**
     * Test getting the count of items in the cart
     */
    @Test
    @DisplayName("Should get cart item count correctly")
    void getCartItemCount_Success() {
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        int count = shoppingCartService.getCartItemCount(testUser);

        assertEquals(1, count);
    }

    /**
     * Test removing all cart items for a specific record
     */
    @Test
    @DisplayName("Should remove all cart items for record successfully")
    void removeAllCartItemsForRecord_Success() {
        List<ShoppingCart> carts = Collections.singletonList(testCart);
        when(shoppingCartRepository.findAll()).thenReturn(carts);
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        shoppingCartService.removeAllCartItemsForRecord(1L);

        verify(shoppingCartRepository).save(testCart);
        assertTrue(testCart.getItems().stream().noneMatch(item -> item.getRecord().getId().equals(1L)));
    }
}
