package com.vintagevinyl.model;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.Getter;

@Data
@Embeddable
public class WishlistItem {
    private String title;
    private String artist;
    @Getter
    private Long recordId;

}