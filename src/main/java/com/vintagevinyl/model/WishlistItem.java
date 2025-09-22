package com.vintagevinyl.model;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.Getter;

/**
 * Represents an item in a wishlist.
 * This class is embeddable and can be used as part of another entity, such as Wishlist.
 */
@Data // Lombok's annotation to generate getters, setters, equals, hashCode, and toString methods automatically.
@Embeddable // Indicates that this class can be embedded in other entities as part of their fields.
public class WishlistItem {

    private String title; // The title of the record added to the wishlist.

    private String artist; // The artist of the record added to the wishlist.

    @Getter // Lombok's annotation to generate a getter method for this field.
    private Long recordId; // The unique identifier of the associated record.
}
