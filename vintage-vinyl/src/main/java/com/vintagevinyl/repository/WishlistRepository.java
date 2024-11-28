package com.vintagevinyl.repository;

import com.vintagevinyl.model.Wishlist;
import com.vintagevinyl.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for managing Wishlist entities.
 *
 * This interface extends JpaRepository, providing CRUD operations
 * and custom query methods for the Wishlist entity.
 */
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /**
     * Custom query method to find a Wishlist by its associated User.
     *
     * @param user the User entity for which the wishlist is to be retrieved
     * @return an Optional containing the Wishlist, or empty if not found
     */
    Optional<Wishlist> findByUser(User user);
}
