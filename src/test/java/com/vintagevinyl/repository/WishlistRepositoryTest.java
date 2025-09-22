package com.vintagevinyl.repository;

import com.vintagevinyl.model.User;
import com.vintagevinyl.model.Wishlist;
import com.vintagevinyl.model.WishlistItem;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WishlistRepositoryTest {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private Wishlist testWishlist;

    @BeforeEach
    void setUp() {
        wishlistRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.setCreatedAt(LocalDateTime.now());
        entityManager.persist(testUser);

        // Create test wishlist item
        WishlistItem testItem = new WishlistItem();
        testItem.setTitle("Test Album");
        testItem.setArtist("Test Artist");
        testItem.setRecordId(1L);

        // Create test wishlist
        testWishlist = new Wishlist();
        testWishlist.setUser(testUser);
        testWishlist.addItem(testItem);

        entityManager.flush();
    }

    /**
     * Test case for saving and retrieving a wishlist.
     */
    @Test
    @DisplayName("Should save and retrieve a wishlist")
    void shouldSaveAndRetrieveWishlist() {
        // Arrange & Act
        Wishlist savedWishlist = wishlistRepository.save(testWishlist);
        Optional<Wishlist> foundWishlist = wishlistRepository.findById(savedWishlist.getId());

        // Assert
        assertThat(foundWishlist)
                .isPresent()
                .hasValueSatisfying(wishlist -> {
                    assertThat(wishlist.getUser()).isEqualTo(testUser);
                    assertThat(wishlist.getItems()).hasSize(1);
                    assertThat(wishlist.getItems().getFirst().getTitle())
                            .isEqualTo("Test Album");
                });
    }

    /**
     * Test case for finding a wishlist by user.
     */
    @Test
    @DisplayName("Should find wishlist by user")
    void shouldFindWishlistByUser() {
        // Arrange
        wishlistRepository.save(testWishlist);

        // Act
        Optional<Wishlist> foundWishlist = wishlistRepository.findByUser(testUser);

        // Assert
        assertThat(foundWishlist)
                .isPresent()
                .hasValueSatisfying(wishlist -> {
                    assertThat(wishlist.getUser()).isEqualTo(testUser);
                    assertThat(wishlist.getItems()).hasSize(1);
                });
    }

    /**
     * Test case for managing wishlist items.
     */
    @Test
    @DisplayName("Should manage wishlist items")
    void shouldManageWishlistItems() {
        // Arrange
        Wishlist wishlist = wishlistRepository.save(testWishlist);

        // Act - Add another item
        WishlistItem newItem = new WishlistItem();
        newItem.setTitle("New Album");
        newItem.setArtist("New Artist");
        newItem.setRecordId(2L);

        wishlist.addItem(newItem);
        Wishlist updatedWishlist = wishlistRepository.save(wishlist);

        // Assert
        assertThat(updatedWishlist.getItems()).hasSize(2);

        // Remove first item
        updatedWishlist.removeItem(0);
        Wishlist finalWishlist = wishlistRepository.save(updatedWishlist);

        assertThat(finalWishlist.getItems())
                .hasSize(1)
                .element(0)
                .satisfies(item -> {
                    assertThat(item.getTitle()).isEqualTo("New Album");
                    assertThat(item.getArtist()).isEqualTo("New Artist");
                });
    }

    /**
     * Test case for handling an empty wishlist.
     */
    @Test
    @DisplayName("Should handle empty wishlist")
    void shouldHandleEmptyWishlist() {
        // Arrange
        Wishlist emptyWishlist = new Wishlist();
        emptyWishlist.setUser(testUser);

        // Act
        Wishlist savedWishlist = wishlistRepository.save(emptyWishlist);

        // Assert
        assertThat(savedWishlist.getItems()).isEmpty();
    }

    /**
     * Test case for handling item removal at invalid indices.
     */
    @Test
    @DisplayName("Should handle item removal at invalid index")
    void shouldHandleInvalidItemRemoval() {
        // Arrange
        Wishlist wishlist = wishlistRepository.save(testWishlist);
        int originalSize = wishlist.getItems().size();

        // Act
        wishlist.removeItem(-1); // Invalid index
        wishlist.removeItem(100); // Invalid index

        // Assert
        assertThat(wishlist.getItems()).hasSize(originalSize);
    }

    /**
     * Test case for handling non-existent wishlists.
     */
    @Test
    @DisplayName("Should find non-existent wishlist")
    void shouldHandleNonExistentWishlist() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newUser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password");
        newUser.setEnabled(true);
        newUser.setCreatedAt(LocalDateTime.now());
        entityManager.persist(newUser);

        // Act & Assert
        assertThat(wishlistRepository.findByUser(newUser)).isEmpty();
        assertThat(wishlistRepository.findById(999L)).isEmpty();
    }

    /**
     * Test case for handling duplicate items in a wishlist.
     */
    @Test
    @DisplayName("Should handle duplicate items")
    void shouldHandleDuplicateItems() {
        // Arrange
        Wishlist wishlist = wishlistRepository.save(testWishlist);

        // Act - Add duplicate item
        WishlistItem duplicateItem = new WishlistItem();
        duplicateItem.setTitle("Test Album");
        duplicateItem.setArtist("Test Artist");
        duplicateItem.setRecordId(1L);

        wishlist.addItem(duplicateItem);
        Wishlist updatedWishlist = wishlistRepository.save(wishlist);

        // Assert
        assertThat(updatedWishlist.getItems())
                .hasSize(2)
                .extracting(WishlistItem::getRecordId)
                .containsExactly(1L, 1L);
    }
}
