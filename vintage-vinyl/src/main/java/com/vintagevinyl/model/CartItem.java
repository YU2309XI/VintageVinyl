package com.vintagevinyl.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Entity representing an item in a shopping cart.
 * Each CartItem is associated with a specific Record and a ShoppingCart.
 * Provides functionality for quantity validation and subtotal calculation.
 */
@Data // Lombok annotation to generate boilerplate code like getters, setters, equals, hashCode, and toString.
@Entity
@Table(name = "cart_items") // Maps this entity to the "cart_items" table in the database.
public class CartItem {

    public static final int MAX_QUANTITY = 99; // Defines the maximum quantity allowed for a single cart item.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates unique IDs for cart items.
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER) // Eagerly fetches associated Record details for immediate access.
    @JoinColumn(name = "record_id") // Specifies the foreign key column for the associated Record.
    private Record record;

    @Column(nullable = false) // Ensures this column cannot be null in the database.
    private Integer quantity = 1; // Default quantity set to 1.

    @ManyToOne // Defines a many-to-one relationship with ShoppingCart.
    @JoinColumn(name = "cart_id", nullable = false) // Specifies the foreign key column for the associated ShoppingCart.
    private ShoppingCart cart;

    /**
     * Calculate the subtotal for this cart item.
     * The subtotal is calculated as the price of the record multiplied by the quantity.
     *
     * @return The subtotal as a BigDecimal, or BigDecimal.ZERO if record or price is unavailable.
     */
    public BigDecimal getSubtotal() {
        // Validate that record and price are available, otherwise return 0 as the subtotal.
        if (record == null || record.getPrice() == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return record.getPrice().multiply(BigDecimal.valueOf(quantity)); // Calculate subtotal.
    }

    /**
     * Set the quantity of the cart item with validation.
     * Ensures the quantity is within the acceptable range (1 to MAX_QUANTITY).
     *
     * @param quantity The desired quantity for this cart item.
     * @throws IllegalArgumentException If the quantity is less than 1 or exceeds MAX_QUANTITY.
     */
    public void setQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1"); // Enforce minimum quantity.
        }
        if (quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity cannot exceed " + MAX_QUANTITY); // Enforce maximum quantity.
        }
        this.quantity = quantity; // Set the validated quantity.
    }
}
