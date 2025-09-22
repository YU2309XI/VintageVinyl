package com.vintagevinyl.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents an order placed by a user.
 * This entity maps to the "orders" table in the database and contains details about the order,
 * including its items, user, and metadata like date, total, and status.
 */
@Data // Lombok generates boilerplate code like getters, setters, and toString automatically.
@Entity
@Table(name = "orders") // Maps the entity to the "orders" database table.
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Primary key with auto-increment strategy.
    private Long id;

    @ManyToOne // Each order is associated with one user.
    @JoinColumn(name = "user_id", nullable = false) // Specifies the foreign key for the "user" table.
    @JsonBackReference // Prevents circular reference issues during JSON serialization.
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    // Maps the relationship to the "order" field in the OrderItem entity.
    @JsonManagedReference // Ensures proper serialization of the items while avoiding infinite loops.
    private List<OrderItem> items = new ArrayList<>(); // A list of items associated with this order.

    @Column(nullable = false) // Ensures the column cannot be null in the database.
    private Date orderDate; // The date and time the order was placed.

    @Column(nullable = false) // Ensures the column cannot be null.
    private BigDecimal total; // The total cost of the order.

    @Column(nullable = false) // Ensures the column cannot be null.
    private String shippingAddress; // The shipping address for the order.

    @Column(nullable = false) // Ensures the column cannot be null.
    private String paymentMethod; // The payment method used for the order (e.g., "Credit Card").

    @Column(nullable = false) // Ensures the column cannot be null.
    private String status; // The current status of the order (e.g., "Pending", "Shipped").
}
