package com.vintagevinyl.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "cart_items")
public class CartItem {
    public static final int MAX_QUANTITY = 99;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "record_id")
    private Record record;

    @Column(nullable = false)
    private Integer quantity = 1;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private ShoppingCart cart;

    // Add methods for price calculations
    public BigDecimal getSubtotal() {
        if (record == null || record.getPrice() == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return record.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    // Validate and set quantity
    public void setQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity cannot exceed " + MAX_QUANTITY);
        }
        this.quantity = quantity;
    }
}