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

@Entity
@Table(name = "records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Column(name = "album")
    private String album;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    private String genre;

    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 5;

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    public void removeCartItem(CartItem item) {
        cartItems.remove(item);
        item.setRecord(null);
    }

    public void setStock(int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        this.stock = newQuantity;
    }

    /**
     * Add to current stock quantity
     * @param quantity Amount to add
     * @throws IllegalArgumentException if quantity is negative
     */
    public void addStock(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Cannot add negative stock quantity");
        }
        this.stock += quantity;
    }

    /**
     * Reduce from current stock quantity
     * @param quantity Amount to reduce
     * @throws IllegalArgumentException if quantity is negative
     * @throws IllegalStateException if insufficient stock
     */
    public void reduceStock(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Cannot reduce negative stock quantity");
        }
        if (this.stock < quantity) {
            throw new IllegalStateException("Insufficient stock");
        }
        this.stock -= quantity;
    }

    /**
     * Check if stock is below threshold
     * @return true if stock is below or equal to threshold
     */
    public boolean isLowStock() {
        return this.stock <= this.lowStockThreshold;
    }

    /**
     * Check if there is sufficient stock
     * @param quantity Amount to check
     * @return true if there is enough stock
     */
    public boolean hasEnoughStock(int quantity) {
        return this.stock >= quantity;
    }
}