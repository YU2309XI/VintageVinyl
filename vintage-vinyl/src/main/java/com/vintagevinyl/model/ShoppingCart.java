package com.vintagevinyl.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * Represents a shopping cart belonging to a user.
 * This entity maps to the "shopping_carts" table in the database and contains
 * the items in the cart as well as methods for managing them.
 */
@Getter // Lombok annotation to generate getter methods for all fields.
@Setter // Lombok annotation to generate setter methods for all fields.
@Entity
@Table(name = "shopping_carts") // Maps this entity to the "shopping_carts" table in the database.
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Primary key with auto-increment strategy.
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) // Defines a one-to-one relationship with the User entity.
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    // Specifies the foreign key referencing the "user" table.
    private User user; // The user to whom this shopping cart belongs.

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    // Defines a one-to-many relationship with CartItem. Changes to the cart cascade to its items.
    private List<CartItem> items = new ArrayList<>(); // List of items in the shopping cart.

    /**
     * Adds an item to the shopping cart.
     * Sets the cart reference in the added item to establish a bidirectional relationship.
     *
     * @param item The cart item to add.
     */
    public void addItem(CartItem item) {
        items.add(item); // Add the item to the list.
        item.setCart(this); // Set the reference to this cart in the item.
    }

    /**
     * Removes an item from the shopping cart.
     * Clears the cart reference in the removed item to maintain data consistency.
     *
     * @param item The cart item to remove.
     */
    public void removeItem(CartItem item) {
        items.remove(item); // Remove the item from the list.
        item.setCart(null); // Clear the reference to this cart in the item.
    }

    /**
     * Calculates the total price of all items in the cart.
     * Iterates over each cart item, multiplying the price of the associated record by its quantity.
     *
     * @return The total price as a BigDecimal.
     */
    public BigDecimal getTotal() {
        return items.stream()
                .map(item -> item.getRecord().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                // Multiply the price of each record by its quantity.
                .reduce(BigDecimal.ZERO, BigDecimal::add); // Sum up the subtotals.
    }

    /**
     * Removes all items related to a specific record from the shopping cart.
     * Useful for handling scenarios where a record is deleted or unavailable.
     *
     * @param recordId The ID of the record whose items should be removed.
     */
    public void removeItemsByRecordId(Long recordId) {
        items.removeIf(item -> item.getRecord().getId().equals(recordId)); // Remove items matching the record ID.
    }
}
