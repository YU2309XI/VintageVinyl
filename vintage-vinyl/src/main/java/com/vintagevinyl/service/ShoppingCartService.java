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


@Service
public class ShoppingCartService {
    private static final Logger logger = LoggerFactory.getLogger(ShoppingCartService.class);

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Transactional
    public void addItemToCart(User user, Long recordId, int quantity) {
        logger.debug("Adding item to cart. User: {}, RecordId: {}, Quantity: {}", user.getId(), recordId, quantity);
        try {
            ShoppingCart cart = getOrCreateCart(user);
            Record record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new RecordNotFoundException("Record not found with id: " + recordId));

            Optional<CartItem> existingItem = cart.getItems().stream()
                    .filter(item -> item.getRecord().getId().equals(recordId))
                    .findFirst();

            if (existingItem.isPresent()) {
                existingItem.get().setQuantity(existingItem.get().getQuantity() + quantity);
                logger.debug("Updated existing item quantity");
            } else {
                CartItem newItem = new CartItem();
                newItem.setRecord(record);
                newItem.setQuantity(quantity);
                newItem.setCart(cart);
                cart.addItem(newItem);
                logger.debug("Added new item to cart");
            }

            shoppingCartRepository.save(cart);
            logger.debug("Cart saved successfully");
        } catch (RecordNotFoundException e) {
            logger.error("Failed to add item to cart: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while adding item to cart", e);
            throw new RuntimeException("Failed to add item to cart", e);
        }
    }

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

    @Transactional(readOnly = true)
    public ShoppingCart getCart(User user) {
        ShoppingCart cart = shoppingCartRepository.findByUser(user)
                .orElseGet(() -> {
                    ShoppingCart newCart = new ShoppingCart();
                    newCart.setUser(user);
                    return shoppingCartRepository.save(newCart);
                });

        cart.getItems().size();

        return cart;
    }

    @Transactional
    public void removeItemFromCart(User user, Long recordId) {
        ShoppingCart cart = getOrCreateCart(user);
        cart.getItems().removeIf(item -> item.getRecord().getId().equals(recordId));
        shoppingCartRepository.save(cart);
    }

    @Transactional
    public void clearCart(User user) {
        ShoppingCart cart = getOrCreateCart(user);
        cart.getItems().clear();
        shoppingCartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateCartTotal(User user) {
        ShoppingCart cart = getCart(user);
        return cart.getItems().stream()
                .map(item -> item.getRecord().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    @Transactional
    public int getCartItemCount(User user) {
        ShoppingCart cart = getOrCreateCart(user);
        return cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    @Transactional
    public void removeAllCartItemsForRecord(Long recordId) {
        List<ShoppingCart> carts = shoppingCartRepository.findAll();
        for (ShoppingCart cart : carts) {
            cart.getItems().removeIf(item -> item.getRecord().getId().equals(recordId));
            shoppingCartRepository.save(cart);
        }
    }
}