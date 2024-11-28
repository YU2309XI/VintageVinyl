package com.vintagevinyl.service;

import com.vintagevinyl.exception.RecordNotFoundException;
import com.vintagevinyl.model.CartItem;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.model.ShoppingCart;
import com.vintagevinyl.model.User;
import com.vintagevinyl.repository.ShoppingCartRepository;
import com.vintagevinyl.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

/**
 * Service class for managing shopping cart operations.
 *
 * This class provides methods to add, update, remove items from the cart,
 * calculate totals, validate cart items, and manage cart data persistence.
 */
@Service
public class ShoppingCartService {

    private static final Logger logger = LoggerFactory.getLogger(ShoppingCartService.class);

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @Autowired
    private RecordRepository recordRepository;

    /**
     * Adds an item to the user's shopping cart.
     *
     * @param user the user who owns the cart
     * @param recordId the ID of the record to add
     * @param quantity the quantity of the record
     * @throws IllegalArgumentException if quantity is invalid
     * @throws IllegalStateException if stock is insufficient
     */
    @Transactional
    public void addItemToCart(User user, Long recordId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (quantity > CartItem.MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity cannot exceed " + CartItem.MAX_QUANTITY);
        }

        try {
            ShoppingCart cart = getOrCreateCart(user);
            Record record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new RecordNotFoundException("Record not found with id: " + recordId));

            // Check stock availability
            if (record.getStock() < quantity) {
                throw new IllegalStateException("Insufficient stock for record: " + record.getTitle());
            }

            Optional<CartItem> existingItem = cart.getItems().stream()
                    .filter(item -> item.getRecord().getId().equals(recordId))
                    .findFirst();

            if (existingItem.isPresent()) {
                existingItem.get().setQuantity(quantity);
            } else {
                CartItem newItem = new CartItem();
                newItem.setRecord(record);
                newItem.setQuantity(quantity);
                newItem.setCart(cart);
                cart.addItem(newItem);
            }

            shoppingCartRepository.save(cart);
        } catch (Exception e) {
            logger.error("Failed to add item to cart", e);
            throw e;
        }
    }

    /**
     * Gets the user's shopping cart, creating a new one if it does not exist.
     *
     * @param user the user whose cart is being accessed
     * @return the shopping cart
     */
    @Transactional
    public ShoppingCart getOrCreateCart(User user) {
        logger.debug("Getting or creating cart for user: {}", user.getUsername());
        return shoppingCartRepository.findByUser(user)
                .orElseGet(() -> {
                    logger.debug("Creating new cart for user: {}", user.getUsername());
                    ShoppingCart newCart = new ShoppingCart();
                    newCart.setUser(user);
                    return shoppingCartRepository.save(newCart);
                });
    }

    /**
     * Retrieves the user's shopping cart, initializing its items.
     *
     * @param user the user whose cart is being accessed
     * @return the initialized shopping cart
     */
    @Transactional(readOnly = true)
    public ShoppingCart getCart(User user) {
        ShoppingCart cart = shoppingCartRepository.findByUser(user)
                .orElseGet(() -> {
                    ShoppingCart newCart = new ShoppingCart();
                    newCart.setUser(user);
                    return shoppingCartRepository.save(newCart);
                });

        // Initialize cart items
        cart.getItems().size();
        return cart;
    }

    /**
     * Removes an item from the user's cart.
     *
     * @param user the user whose cart is being modified
     * @param recordId the ID of the record to remove
     */
    @Transactional
    public void removeItemFromCart(User user, Long recordId) {
        ShoppingCart cart = getOrCreateCart(user);
        cart.getItems().removeIf(item -> item.getRecord().getId().equals(recordId));
        shoppingCartRepository.save(cart);
    }

    /**
     * Clears all items from the user's cart.
     *
     * @param user the user whose cart is being cleared
     */
    @Transactional
    public void clearCart(User user) {
        ShoppingCart cart = getOrCreateCart(user);
        cart.getItems().removeIf(item -> true);
        shoppingCartRepository.save(cart);
        shoppingCartRepository.flush();
    }

    /**
     * Calculates the total cost of the items in the user's cart.
     *
     * @param user the user whose cart total is being calculated
     * @return the total cost as a BigDecimal
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCartTotal(User user) {
        ShoppingCart cart = getCart(user);
        if (cart == null || cart.getItems() == null) {
            return BigDecimal.ZERO;
        }

        return cart.getItems().stream()
                .map(item -> item.getRecord().getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Updates the quantity of an item in the user's cart.
     *
     * @param user the user whose cart is being updated
     * @param recordId the ID of the record
     * @param quantity the new quantity
     * @throws IllegalArgumentException if quantity is invalid
     */
    @Transactional
    public void updateCartItemQuantity(User user, Long recordId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (quantity > CartItem.MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity cannot exceed " + CartItem.MAX_QUANTITY);
        }

        ShoppingCart cart = getOrCreateCart(user);
        Optional<CartItem> item = cart.getItems().stream()
                .filter(i -> i.getRecord().getId().equals(recordId))
                .findFirst();

        if (item.isPresent()) {
            item.get().setQuantity(quantity);
            shoppingCartRepository.save(cart);
        } else {
            throw new RuntimeException("Item not found in cart");
        }
    }

    /**
     * Validates the items in the user's cart, removing invalid items.
     *
     * @param user the user whose cart is being validated
     */
    @Transactional
    public void validateCartItems(User user) {
        ShoppingCart cart = getCart(user);
        boolean hasChanges = cart.getItems().removeIf(item ->
                item.getRecord() == null ||
                        item.getQuantity() == null ||
                        item.getQuantity() < 1
        );

        if (hasChanges) {
            shoppingCartRepository.save(cart);
        }
    }

    /**
     * Counts the total number of items in the user's cart.
     *
     * @param user the user whose cart is being counted
     * @return the total number of items
     */
    @Transactional
    public int getCartItemCount(User user) {
        ShoppingCart cart = getOrCreateCart(user);
        return cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Removes all cart items referencing a specific record from all carts.
     *
     * @param recordId the ID of the record to remove
     */
    @Transactional
    public void removeAllCartItemsForRecord(Long recordId) {
        List<ShoppingCart> carts = shoppingCartRepository.findAll();
        for (ShoppingCart cart : carts) {
            cart.getItems().removeIf(item -> item.getRecord().getId().equals(recordId));
            shoppingCartRepository.save(cart);
        }
    }
}
