package com.vintagevinyl.service;

import com.vintagevinyl.model.Wishlist;
import com.vintagevinyl.model.User;
import com.vintagevinyl.model.WishlistItem;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    public Wishlist getWishlistForUser(User user) {
        return wishlistRepository.findByUser(user)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setUser(user);
                    newWishlist.setItems(new ArrayList<>());
                    return wishlistRepository.save(newWishlist);
                });
    }

    @Transactional
    public void addToWishlist(User user, Record record) {
        Wishlist wishlist = getWishlistForUser(user);
        WishlistItem item = new WishlistItem();
        item.setTitle(record.getTitle());
        item.setArtist(record.getArtist());
        item.setRecordId(record.getId());
        wishlist.getItems().add(item);
        wishlistRepository.save(wishlist);
    }

    public void removeItemFromWishlist(User user, int itemIndex) {
        Wishlist wishlist = getWishlistForUser(user);
        if (itemIndex >= 0 && itemIndex < wishlist.getItems().size()) {
            wishlist.getItems().remove(itemIndex);
            wishlistRepository.save(wishlist);
        }
    }
}