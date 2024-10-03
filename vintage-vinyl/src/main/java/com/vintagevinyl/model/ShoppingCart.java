package com.vintagevinyl.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "shopping_carts")
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItem> items = new ArrayList<>();

    // Helper method to add item to cart
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    // Helper method to remove item from cart
    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    // Method to calculate the total price of the cart
    public BigDecimal getTotal() {
        return items.stream()
                .map(item -> item.getRecord().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Helper method to remove all items related to a specific record
    public void removeItemsByRecordId(Long recordId) {
        items.removeIf(item -> item.getRecord().getId().equals(recordId));
    }
}