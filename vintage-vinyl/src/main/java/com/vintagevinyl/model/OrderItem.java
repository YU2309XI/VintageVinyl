package com.vintagevinyl.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Represents an individual item in an order.
 * This entity maps to the "order_items" table in the database and contains details about
 * the record, quantity, and price of the item in a specific order.
 */
@Data // Lombok annotation to generate getters, setters, equals, hashCode, and toString methods automatically.
@Entity
@Table(name = "order_items") // Maps this entity to the "order_items" table in the database.
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Primary key with auto-increment strategy.
    private Long id;

    @ManyToOne // Defines a many-to-one relationship with the Order entity.
    @JoinColumn(name = "order_id", nullable = false) // Foreign key referencing the "orders" table.
    @JsonBackReference // Prevents circular reference issues during JSON serialization.
    private Order order; // The order to which this item belongs.

    @ManyToOne // Defines a many-to-one relationship with the Record entity.
    @JoinColumn(name = "record_id", nullable = false) // Foreign key referencing the "records" table.
    private Record record; // The specific record associated with this order item.

    @Column(nullable = false) // Ensures the column cannot be null in the database.
    private Integer quantity; // The quantity of the record ordered.

    @Column(nullable = false) // Ensures the column cannot be null in the database.
    private BigDecimal price; // The price of the record at the time of the order.
}
