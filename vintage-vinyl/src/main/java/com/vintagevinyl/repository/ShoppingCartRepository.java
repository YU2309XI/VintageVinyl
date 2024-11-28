package com.vintagevinyl.repository;

import com.vintagevinyl.model.ShoppingCart;
import com.vintagevinyl.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing ShoppingCart entities.
 *
 * This interface extends JpaRepository, providing CRUD operations
 * and query methods for the ShoppingCart entity.
 */
@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {

    /**
     * Custom query method to find a ShoppingCart by its associated User.
     *
     * @param user the User entity for which the shopping cart is to be retrieved
     * @return an Optional containing the ShoppingCart, or empty if not found
     */
    Optional<ShoppingCart> findByUser(User user);
}
