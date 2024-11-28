package com.vintagevinyl.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a record in the inventory.
 * This entity maps to the "records" table in the database and contains details such as title, artist,
 * album, release date, price, stock levels, and associated cart items.
 */
@Entity
@Table(name = "records") // Maps this entity to the "records" table in the database.
@Getter // Lombok annotation to generate getter methods for all fields.
@Setter // Lombok annotation to generate setter methods for all fields.
@NoArgsConstructor // Lombok annotation to generate a no-argument constructor.
@AllArgsConstructor // Lombok annotation to generate a constructor with all fields as arguments.
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Primary key with auto-increment strategy.
    private Long id;

    @Column(nullable = false) // Ensures this column cannot be null in the database.
    private String title; // The title of the record.

    @Column(nullable = false) // Ensures this column cannot be null in the database.
    private String artist; // The artist of the record.

    @Column(name = "album") // Maps this field to the "album" column in the database.
    private String album; // The album name associated with this record.

    @Column(name = "release_date") // Maps this field to the "release_date" column in the database.
    private LocalDate releaseDate; // The release date of the record.

    @Column(name = "cover_image_url") // Maps this field to the "cover_image_url" column in the database.
    private String coverImageUrl; // The URL for the cover image of the record.

    private String genre; // The genre of the record.

    private BigDecimal price; // The price of the record.

    @Column(nullable = false) // Ensures this column cannot be null in the database.
    private Integer stock = 0; // The current stock quantity for the record, defaulting to 0.

    @Column(name = "low_stock_threshold") // Maps this field to the "low_stock_threshold" column in the database.
    private Integer lowStockThreshold = 5; // The threshold below which stock is considered low.

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    // Maps the relationship to the "record" field in the CartItem entity.
    private List<CartItem> cartItems = new ArrayList<>(); // List of cart items associated with this record.

    /**
     * Removes a cart item associated with this record.
     * @param item The cart item to remove.
     */
    public void removeCartItem(CartItem item) {
        cartItems.remove(item); // Remove the cart item from the list.
        item.setRecord(null); // Disassociate the cart item from this record.
    }

    /**
     * Sets the stock quantity for the record.
     * Ensures the stock quantity is non-negative.
     * @param newQuantity The new stock quantity.
     * @throws IllegalArgumentException if the new quantity is negative.
     */
    public void setStock(int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        this.stock = newQuantity;
    }

    /**
     * Adds to the current stock quantity.
     * @param quantity The amount to add.
     * @throws IllegalArgumentException if the quantity is negative.
     */
    public void addStock(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Cannot add negative stock quantity");
        }
        this.stock += quantity; // Increment the stock by the specified quantity.
    }

    /**
     * Reduces the current stock quantity.
     * Ensures the stock remains non-negative after the reduction.
     * @param quantity The amount to reduce.
     * @throws IllegalArgumentException if the quantity is negative.
     * @throws IllegalStateException if there is insufficient stock to fulfill the reduction.
     */
    public void reduceStock(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Cannot reduce negative stock quantity");
        }
        if (this.stock < quantity) {
            throw new IllegalStateException("Insufficient stock");
        }
        this.stock -= quantity; // Decrement the stock by the specified quantity.
    }

    /**
     * Checks if the stock quantity is below the low-stock threshold.
     * @return true if the stock is below or equal to the threshold, false otherwise.
     */
    public boolean isLowStock() {
        return this.stock <= this.lowStockThreshold;
    }

    /**
     * Checks if there is sufficient stock to fulfill the specified quantity.
     * @param quantity The amount to check.
     * @return true if there is enough stock, false otherwise.
     */
    public boolean hasEnoughStock(int quantity) {
        return this.stock >= quantity;
    }
}
