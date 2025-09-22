package com.vintagevinyl.integration;

import com.vintagevinyl.model.*;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.service.WishlistService;
import com.vintagevinyl.service.UserService;
import com.vintagevinyl.repository.RecordRepository;
import com.vintagevinyl.repository.WishlistRepository;
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
 * Integration tests for wishlist functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WishlistIntegrationTest {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private UserService userService;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

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
        user.addRole("USER");
        testUser = userService.save(user);

        // Create test record
        Record record = new Record();
        record.setTitle("Test Album");
        record.setArtist("Test Artist");
        record.setPrice(new BigDecimal("29.99"));
        record.setStock(10);
        record.setReleaseDate(LocalDate.now());
        testRecord = recordRepository.save(record);
    }

    /**
     * Test adding item to wishlist
     */
    @Test
    void addItemToWishlist() {
        // Add item to wishlist
        wishlistService.addToWishlist(testUser, testRecord);

        // Get wishlist
        Wishlist wishlist = wishlistService.getWishlistForUser(testUser);

        // Verify wishlist state
        assertNotNull(wishlist, "Wishlist should not be null");
        assertEquals(1, wishlist.getItems().size(), "Wishlist should contain one item");

        // Verify item details
        WishlistItem item = wishlist.getItems().getFirst();
        assertEquals(testRecord.getTitle(), item.getTitle(), "Item title should match");
        assertEquals(testRecord.getArtist(), item.getArtist(), "Item artist should match");
        assertEquals(testRecord.getId(), item.getRecordId(), "Item record ID should match");
    }

    /**
     * Test adding multiple items to wishlist
     */
    @Test
    void addMultipleItemsToWishlist() {
        // Create second test record
        Record secondRecord = new Record();
        secondRecord.setTitle("Second Album");
        secondRecord.setArtist("Second Artist");
        secondRecord.setPrice(new BigDecimal("19.99"));
        secondRecord.setStock(5);
        secondRecord = recordRepository.save(secondRecord);

        // Add items to wishlist
        wishlistService.addToWishlist(testUser, testRecord);
        wishlistService.addToWishlist(testUser, secondRecord);

        // Get wishlist
        Wishlist wishlist = wishlistService.getWishlistForUser(testUser);

        // Verify wishlist state
        assertEquals(2, wishlist.getItems().size(), "Wishlist should contain two items");

        // Verify second item details
        WishlistItem secondItem = wishlist.getItems().get(1);
        assertEquals(secondRecord.getTitle(), secondItem.getTitle(), "Second item title should match");
        assertEquals(secondRecord.getArtist(), secondItem.getArtist(), "Second item artist should match");
    }

    /**
     * Test removing item from wishlist
     */
    @Test
    void removeItemFromWishlist() {
        // Add item to wishlist
        wishlistService.addToWishlist(testUser, testRecord);

        // Remove item
        Wishlist wishlist = wishlistService.getWishlistForUser(testUser);
        wishlistService.removeItemFromWishlist(testUser, 0);

        // Verify wishlist is empty
        wishlist = wishlistService.getWishlistForUser(testUser);
        assertTrue(wishlist.getItems().isEmpty(), "Wishlist should be empty after removal");
    }

    /**
     * Test getting wishlist for new user
     */
    @Test
    void getWishlistForNewUser() {
        // Get a wishlist for new user
        Wishlist wishlist = wishlistService.getWishlistForUser(testUser);

        // Verify new wishlist state
        assertNotNull(wishlist, "New user should get an empty wishlist");
        assertNotNull(wishlist.getId(), "Wishlist should be persisted");
        assertTrue(wishlist.getItems().isEmpty(), "New wishlist should be empty");
        assertEquals(testUser, wishlist.getUser(), "Wishlist should be associated with user");
    }

    /**
     * Test removing item with invalid index
     */
    @Test
    void removeItemWithInvalidIndex() {
        // Add item to wishlist
        wishlistService.addToWishlist(testUser, testRecord);

        // Attempt to remove with invalid index
        wishlistService.removeItemFromWishlist(testUser, 999);

        // Verify item still exists
        Wishlist wishlist = wishlistService.getWishlistForUser(testUser);
        assertEquals(1, wishlist.getItems().size(), "Item should not be removed with invalid index");
    }
}