package com.vintagevinyl.service;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.model.User;
import com.vintagevinyl.model.Wishlist;
import com.vintagevinyl.model.WishlistItem;
import com.vintagevinyl.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private User testUser;
    private Record testRecord;
    private Wishlist testWishlist;

    @BeforeEach
    void setUp() {
        // Initialize test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");

        // Initialize test record
        testRecord = new Record();
        testRecord.setId(1L);
        testRecord.setTitle("Test Album");
        testRecord.setArtist("Test Artist");
        testRecord.setPrice(new BigDecimal("29.99"));

        // Initialize test wishlist item
        WishlistItem testWishlistItem = new WishlistItem();
        testWishlistItem.setTitle(testRecord.getTitle());
        testWishlistItem.setArtist(testRecord.getArtist());
        testWishlistItem.setRecordId(testRecord.getId());

        // Initialize test wishlist
        testWishlist = new Wishlist();
        testWishlist.setId(1L);
        testWishlist.setUser(testUser);
        testWishlist.setItems(new ArrayList<>());
        testWishlist.addItem(testWishlistItem);
    }

    /**
     * Test case for retrieving an existing wishlist for a user.
     */
    @Test
    @DisplayName("Should get existing wishlist for user")
    void getWishlistForUser_ExistingWishlist_Success() {
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));

        Wishlist result = wishlistService.getWishlistForUser(testUser);

        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(1, result.getItems().size());
        verify(wishlistRepository).findByUser(testUser);
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    /**
     * Test case for creating a new wishlist when none exists for the user.
     */
    @Test
    @DisplayName("Should create new wishlist for user when not exists")
    void getWishlistForUser_NewWishlist_Success() {
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(i -> {
            Wishlist w = i.getArgument(0);
            w.setId(1L);
            return w;
        });

        Wishlist result = wishlistService.getWishlistForUser(testUser);

        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertTrue(result.getItems().isEmpty());
        verify(wishlistRepository).findByUser(testUser);
        verify(wishlistRepository).save(any(Wishlist.class));
    }

    /**
     * Test case for successfully adding an item to the wishlist.
     */
    @Test
    @DisplayName("Should add item to wishlist successfully")
    void addToWishlist_Success() {
        Wishlist emptyWishlist = new Wishlist();
        emptyWishlist.setUser(testUser);
        emptyWishlist.setItems(new ArrayList<>());

        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(emptyWishlist));
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(emptyWishlist);

        wishlistService.addToWishlist(testUser, testRecord);

        assertEquals(1, emptyWishlist.getItems().size());
        WishlistItem addedItem = emptyWishlist.getItems().getFirst();
        assertEquals(testRecord.getTitle(), addedItem.getTitle());
        assertEquals(testRecord.getArtist(), addedItem.getArtist());
        assertEquals(testRecord.getId(), addedItem.getRecordId());
        verify(wishlistRepository).save(emptyWishlist);
    }

    /**
     * Test case for successfully removing an item from the wishlist by a valid index.
     */
    @Test
    @DisplayName("Should remove item from wishlist successfully")
    void removeItemFromWishlist_ValidIndex_Success() {
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(testWishlist);

        wishlistService.removeItemFromWishlist(testUser, 0);

        assertTrue(testWishlist.getItems().isEmpty());
        verify(wishlistRepository).save(testWishlist);
    }

    /**
     * Test case for ensuring no item is removed when the index is invalid.
     */
    @Test
    @DisplayName("Should not remove item when index is invalid")
    void removeItemFromWishlist_InvalidIndex_NoAction() {
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));

        wishlistService.removeItemFromWishlist(testUser, 999);

        assertEquals(1, testWishlist.getItems().size());
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    /**
     * Test case for ensuring no item is removed when the index is negative.
     */
    @Test
    @DisplayName("Should not remove item when index is negative")
    void removeItemFromWishlist_NegativeIndex_NoAction() {
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));

        wishlistService.removeItemFromWishlist(testUser, -1);

        assertEquals(1, testWishlist.getItems().size());
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    /**
     * Test case for successfully adding multiple items to the wishlist.
     */
    @Test
    @DisplayName("Should handle adding multiple items to wishlist")
    void addToWishlist_MultipleItems_Success() {
        Wishlist emptyWishlist = new Wishlist();
        emptyWishlist.setUser(testUser);
        emptyWishlist.setItems(new ArrayList<>());

        Record secondRecord = new Record();
        secondRecord.setId(2L);
        secondRecord.setTitle("Second Album");
        secondRecord.setArtist("Second Artist");

        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(emptyWishlist));
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(emptyWishlist);

        wishlistService.addToWishlist(testUser, testRecord);
        wishlistService.addToWishlist(testUser, secondRecord);

        assertEquals(2, emptyWishlist.getItems().size());
        assertEquals(testRecord.getTitle(), emptyWishlist.getItems().get(0).getTitle());
        assertEquals(secondRecord.getTitle(), emptyWishlist.getItems().get(1).getTitle());
        verify(wishlistRepository, times(2)).save(emptyWishlist);
    }
}
