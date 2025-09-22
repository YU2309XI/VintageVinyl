package com.vintagevinyl.repository;

import com.vintagevinyl.model.CartItem;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.model.ShoppingCart;
import com.vintagevinyl.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest  // JPA component testing
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ShoppingCartRepositoryTest {

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private Record testRecord;
    private ShoppingCart testCart;

    @BeforeEach
    void setUp() {
        shoppingCartRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.setCreatedAt(LocalDateTime.now());
        entityManager.persist(testUser);

        // Create test record
        testRecord = new Record();
        testRecord.setTitle("Test Album");
        testRecord.setArtist("Test Artist");
        testRecord.setPrice(BigDecimal.valueOf(29.99));
        testRecord.setStock(10);
        entityManager.persist(testRecord);

        // Create test cart
        testCart = new ShoppingCart();
        testCart.setUser(testUser);

        CartItem cartItem = new CartItem();
        cartItem.setRecord(testRecord);
        cartItem.setQuantity(1);
        cartItem.setCart(testCart);

        testCart.addItem(cartItem);

        entityManager.flush();
    }

    /**
     * Test case for saving and retrieving a shopping cart.
     */
    @Test
    @DisplayName("Should save and retrieve a shopping cart")
    void shouldSaveAndRetrieveCart() {
        // Arrange & Act
        ShoppingCart savedCart = shoppingCartRepository.save(testCart);
        Optional<ShoppingCart> foundCart = shoppingCartRepository.findById(savedCart.getId());

        // Assert
        assertThat(foundCart)
                .isPresent()
                .hasValueSatisfying(cart -> {
                    assertThat(cart.getUser()).isEqualTo(testUser);
                    assertThat(cart.getItems()).hasSize(1);
                    assertThat(cart.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(29.99));
                });
    }

    /**
     * Test case for finding a shopping cart by user.
     */
    @Test
    @DisplayName("Should find cart by user")
    void shouldFindCartByUser() {
        // Arrange
        shoppingCartRepository.save(testCart);

        // Act
        Optional<ShoppingCart> foundCart = shoppingCartRepository.findByUser(testUser);

        // Assert
        assertThat(foundCart)
                .isPresent()
                .hasValueSatisfying(cart -> {
                    assertThat(cart.getUser()).isEqualTo(testUser);
                    assertThat(cart.getItems()).hasSize(1);
                });
    }

    /**
     * Test case for managing items in the shopping cart.
     */
    @Test
    @DisplayName("Should manage cart items")
    void shouldManageCartItems() {
        // Arrange
        ShoppingCart cart = shoppingCartRepository.save(testCart);

        // Act - Add another item
        Record newRecord = new Record();
        newRecord.setTitle("New Album");
        newRecord.setArtist("New Artist");
        newRecord.setPrice(BigDecimal.valueOf(19.99));
        newRecord.setStock(5);
        entityManager.persist(newRecord);

        CartItem newItem = new CartItem();
        newItem.setRecord(newRecord);
        newItem.setQuantity(2);
        newItem.setCart(cart);

        cart.addItem(newItem);

        ShoppingCart updatedCart = shoppingCartRepository.save(cart);

        // Assert
        assertThat(updatedCart.getItems()).hasSize(2);
        assertThat(updatedCart.getTotal())
                .isEqualByComparingTo(BigDecimal.valueOf(69.97)); // 29.99 + (19.99 * 2)
    }

    /**
     * Test case for enforcing constraints on cart item quantities.
     */
    @Test
    @DisplayName("Should enforce cart item quantity constraints")
    void shouldEnforceCartItemQuantityConstraints() {
        // Arrange
        CartItem item = new CartItem();
        item.setRecord(testRecord);

        // Assert
        assertThatThrownBy(() -> item.setQuantity(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be at least 1");

        assertThatThrownBy(() -> item.setQuantity(CartItem.MAX_QUANTITY + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity cannot exceed " + CartItem.MAX_QUANTITY);
    }

    /**
     * Test case for removing items from the cart by record ID.
     */
    @Test
    @DisplayName("Should remove items by record ID")
    void shouldRemoveItemsByRecordId() {
        // Arrange
        ShoppingCart cart = shoppingCartRepository.save(testCart);
        Long recordId = testRecord.getId();

        // Act
        cart.removeItemsByRecordId(recordId);
        ShoppingCart updatedCart = shoppingCartRepository.save(cart);

        // Assert
        assertThat(updatedCart.getItems()).isEmpty();
        assertThat(updatedCart.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * Test case for calculating the correct subtotal for a cart item.
     */
    @Test
    @DisplayName("Should calculate correct subtotal for cart item")
    void shouldCalculateCorrectSubtotal() {
        // Arrange
        CartItem item = testCart.getItems().getFirst();
        item.setQuantity(3);

        // Act
        BigDecimal subtotal = item.getSubtotal();

        // Assert
        assertThat(subtotal)
                .isEqualByComparingTo(BigDecimal.valueOf(89.97)); // 29.99 * 3
    }

    /**
     * Test case for handling cart items with null records or prices.
     */
    @Test
    @DisplayName("Should handle cart item with null record or price")
    void shouldHandleCartItemWithNullRecordOrPrice() {
        // Arrange
        CartItem item = new CartItem();

        // Assert
        assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);

        item.setRecord(new Record()); // Record without a price
        assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
