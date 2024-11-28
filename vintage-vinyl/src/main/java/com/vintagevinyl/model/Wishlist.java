package com.vintagevinyl.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a wishlist associated with a user.
 * This entity maps to the "wishlists" table in the database and contains a list of wishlist items.
 */
@Data // Lombok annotation to generate getters, setters, equals, hashCode, and toString methods automatically.
@Entity
@Table(name = "wishlists") // Maps this entity to the "wishlists" table in the database.
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Primary key with auto-increment strategy.
    private Long id;

    @OneToOne // Defines a one-to-one relationship with the User entity.
    @JoinColumn(name = "user_id", nullable = false) // Specifies the foreign key referencing the "users" table.
    private User user; // The user to whom this wishlist belongs.

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wishlist_items", joinColumns = @JoinColumn(name = "wishlist_id"))
    // Defines a collection of wishlist items, stored in the "wishlist_items" table.
    private List<WishlistItem> items = new ArrayList<>(); // List of items in the wishlist.

    /**
     * Adds an item to the wishlist.
     *
     * @param item The wishlist item to add.
     */
    public void addItem(WishlistItem item) {
        items.add(item); // Add the item to the list.
    }

    /**
     * Removes an item from the wishlist by its index.
     * Validates the index to prevent out-of-bounds errors.
     *
     * @param index The index of the item to remove.
     */
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) { // Validate the index.
            items.remove(index); // Remove the item from the list.
        }
    }
}
