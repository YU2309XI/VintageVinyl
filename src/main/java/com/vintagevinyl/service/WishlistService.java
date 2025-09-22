package com.vintagevinyl.service;

import com.vintagevinyl.model.Wishlist;
import com.vintagevinyl.model.User;
import com.vintagevinyl.model.WishlistItem;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * Service class for managing user wishlists.
 *
 * This class provides functionality to retrieve, add, and remove items from a user's wishlist.
 */
@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    /**
     * Retrieves the wishlist for a specific user. If the user does not have an existing wishlist,
     * a new one is created and saved to the database.
     *
     * @param user the user whose wishlist is being retrieved
     * @return the Wishlist entity associated with the user
     */
    public Wishlist getWishlistForUser(User user) {
        return wishlistRepository.findByUser(user)
                .orElseGet(() -> {
                    // Create a new wishlist for the user if one does not exist
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setUser(user);
                    newWishlist.setItems(new ArrayList<>());
                    return wishlistRepository.save(newWishlist);
                });
    }

    /**
     * Adds a record to the user's wishlist. If the wishlist does not exist, it will be created.
     *
     * @param user the user whose wishlist is being updated
     * @param record the record to add to the wishlist
     */
    @Transactional
    public void addToWishlist(User user, Record record) {
        Wishlist wishlist = getWishlistForUser(user);

        // Create a new WishlistItem and add it to the user's wishlist
        WishlistItem item = new WishlistItem();
        item.setTitle(record.getTitle());
        item.setArtist(record.getArtist());
        item.setRecordId(record.getId());
        wishlist.getItems().add(item);

        // Save the updated wishlist
        wishlistRepository.save(wishlist);
    }

    /**
     * Removes an item from the user's wishlist by its index.
     *
     * @param user the user whose wishlist is being updated
     * @param itemIndex the index of the item to remove
     */
    public void removeItemFromWishlist(User user, int itemIndex) {
        Wishlist wishlist = getWishlistForUser(user);

        // Check if the index is within the valid range
        if (itemIndex >= 0 && itemIndex < wishlist.getItems().size()) {
            wishlist.getItems().remove(itemIndex);

            // Save the updated wishlist
            wishlistRepository.save(wishlist);
        }
    }
}
