package com.vintagevinyl.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class WishlistItem {
    private String title;
    private String artist;
}