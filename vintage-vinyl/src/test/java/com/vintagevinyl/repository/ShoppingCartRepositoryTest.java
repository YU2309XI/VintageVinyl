package com.vintagevinyl.repository;

import com.vintagevinyl.model.*;
import com.vintagevinyl.model.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ShoppingCartRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    private User testUser;
    private Record testRecord;
    private ShoppingCart testCart;

    @BeforeEach
    void setUp() {
        // Create a test user with a random email to avoid conflicts
        testUser = new User();
        testUser.setUsername("testUser" + UUID.randomUUID());
        testUser.setPassword("password");
        testUser.setEmail("test" + UUID.randomUUID() + "@example.com");
        entityManager.persist(testUser);

        // Create a test record
        testRecord = new Record();
        testRecord.setTitle("Test Record");
        testRecord.setArtist("Test Artist");
        testRecord.setPrice(new BigDecimal("29.99"));
        entityManager.persist(testRecord);

        // Create a shopping cart
        testCart = new ShoppingCart();
        testCart.setUser(testUser);
        entityManager.persist(testCart);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByUserShouldReturnCart() {
        Optional<ShoppingCart> found = shoppingCartRepository.findByUser(testUser);
        assertTrue(found.isPresent());
        assertEquals(testUser.getId(), found.get().getUser().getId());
    }

    @Test
    void findByUserWithItemsShouldReturnCartAndItems() {
        // First, retrieve the persisted cart from the database
        ShoppingCart managedCart = shoppingCartRepository.findByUser(testUser).orElseThrow();

        // Create a cart item and set the relationship
        CartItem item = new CartItem();
        item.setRecord(testRecord);
        item.setQuantity(1);
        item.setCart(managedCart);

        // Use bidirectional association
        managedCart.getItems().add(item);

        // Save the updated cart
        shoppingCartRepository.save(managedCart);

        entityManager.flush();
        entityManager.clear();

        // Validate the result
        Optional<ShoppingCart> found = shoppingCartRepository.findByUser(testUser);
        assertTrue(found.isPresent());
        assertFalse(found.get().getItems().isEmpty(), "Cart should have items");
        assertEquals(1, found.get().getItems().size());
        CartItem foundItem = found.get().getItems().iterator().next();
        assertEquals(testRecord.getId(), foundItem.getRecord().getId());
    }

    @Test
    void saveShouldPersistNewCart() {
        // Create a new user
        User newUser = new User();
        newUser.setUsername("newUser" + UUID.randomUUID());
        newUser.setPassword("password");
        newUser.setEmail("new" + UUID.randomUUID() + "@example.com");
        entityManager.persist(newUser);

        // Create a new shopping cart
        ShoppingCart newCart = new ShoppingCart();
        newCart.setUser(newUser);

        // Save the cart
        ShoppingCart saved = shoppingCartRepository.save(newCart);
        entityManager.flush();
        entityManager.clear();

        // Validate
        assertNotNull(saved.getId());
        assertEquals(newUser.getId(), saved.getUser().getId());

        // Reload from the database to verify
        ShoppingCart found = entityManager.find(ShoppingCart.class, saved.getId());
        assertNotNull(found);
        assertEquals(newUser.getId(), found.getUser().getId());
    }

    @Test
    void addItemToCartShouldPersist() {
        // Retrieve the persisted cart
        ShoppingCart managedCart = shoppingCartRepository.findByUser(testUser).orElseThrow();

        // Add a cart item
        CartItem item = new CartItem();
        item.setRecord(testRecord);
        item.setQuantity(1);
        item.setCart(managedCart);
        managedCart.getItems().add(item);

        // Save the updated cart
        shoppingCartRepository.save(managedCart);
        entityManager.flush();
        entityManager.clear();

        // Validate
        ShoppingCart found = entityManager.find(ShoppingCart.class, managedCart.getId());
        assertNotNull(found);
        assertEquals(1, found.getItems().size());
        assertEquals(testRecord.getId(), found.getItems().iterator().next().getRecord().getId());
    }

    @Test
    void deleteCartShouldRemoveCartAndItems() {
        // Retrieve the persisted cart
        ShoppingCart managedCart = shoppingCartRepository.findByUser(testUser).orElseThrow();

        // Add a cart item
        CartItem item = new CartItem();
        item.setRecord(testRecord);
        item.setQuantity(1);
        item.setCart(managedCart);
        managedCart.getItems().add(item);

        shoppingCartRepository.save(managedCart);
        entityManager.flush();

        Long cartId = managedCart.getId();
        Long itemId = managedCart.getItems().iterator().next().getId();

        // Delete the cart
        shoppingCartRepository.delete(managedCart);
        entityManager.flush();
        entityManager.clear();

        // Verify the cart was deleted
        ShoppingCart found = entityManager.find(ShoppingCart.class, cartId);
        assertNull(found);

        // Verify the cart item was also deleted
        CartItem foundItem = entityManager.find(CartItem.class, itemId);
        assertNull(foundItem);
    }
}
