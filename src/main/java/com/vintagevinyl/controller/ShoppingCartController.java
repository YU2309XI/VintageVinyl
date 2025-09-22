package com.vintagevinyl.controller;

import com.vintagevinyl.model.ShoppingCart;
import com.vintagevinyl.model.User;
import com.vintagevinyl.service.ShoppingCartService;
import com.vintagevinyl.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * Controller for managing shopping cart operations.
 * This includes adding, removing, viewing, and clearing cart items, as well as calculating totals.
 */
@Controller
@RequestMapping("/cart")
public class ShoppingCartController {
    private static final Logger logger = LoggerFactory.getLogger(ShoppingCartController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * Display the shopping cart for the authenticated user.
     * Ensures that cart items are fully loaded to avoid lazy initialization exceptions in the view.
     */
    @Transactional // Ensures the database session is active during lazy loading of cart items.
    @GetMapping
    public String viewCart(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login"; // Redirect unauthenticated users to the login page.
        }

        // Fetch the authenticated user from the database.
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            logger.error("User not found in database");
            return "redirect:/login";
        }

        // Retrieve the user's shopping cart and initialize lazy-loaded items.
        ShoppingCart cart = shoppingCartService.getCart(user);
        cart.getItems().forEach(item -> item.getRecord().getTitle()); // Ensures cart items are loaded.

        model.addAttribute("cart", cart); // Pass the cart to the view for rendering.
        return "cart"; // Return the cart view.
    }

    /**
     * Add a record to the user's shopping cart.
     * Redirects to the wishlist after adding the item, ensuring that changes are reflected immediately.
     */
    @PostMapping("/add")
    public String addToCart(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam("recordId") Long recordId,
                            @RequestParam(defaultValue = "1") int quantity,
                            RedirectAttributes redirectAttributes) {
        logger.debug("Received add to cart request: recordId={}, quantity={}", recordId, quantity);

        if (userDetails == null) {
            return "redirect:/login"; // Only authenticated users can add to cart.
        }

        try {
            User user = userService.findByUsername(userDetails.getUsername());
            if (user == null) {
                logger.error("User not found in database");
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/wishlist";
            }

            // Add the specified record to the user's cart with the given quantity.
            shoppingCartService.addItemToCart(user, recordId, quantity);
            redirectAttributes.addFlashAttribute("message", "Item added to cart successfully");
        } catch (Exception e) {
            logger.error("Error adding item to cart", e);
            redirectAttributes.addFlashAttribute("error", "Failed to add record to cart: " + e.getMessage());
        }
        return "redirect:/wishlist";
    }

    /**
     * Remove an item from the user's shopping cart.
     * Handles any exceptions and provides user feedback through flash attributes.
     */
    @PostMapping("/remove")
    public String removeFromCart(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam Long recordId,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            shoppingCartService.removeItemFromCart(user, recordId);
            redirectAttributes.addFlashAttribute("message", "Item removed from cart successfully.");
        } catch (Exception e) {
            logger.error("Error removing item from cart", e);
            redirectAttributes.addFlashAttribute("error", "Failed to remove item from cart: " + e.getMessage());
        }
        return "redirect:/cart";
    }

    /**
     * Clear all items from the user's shopping cart.
     * Provides a JSON response indicating the success or failure of the operation.
     */
    @PostMapping("/clear")
    @ResponseBody // Indicates that this method returns a JSON response instead of rendering a view.
    public ResponseEntity<String> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Attempting to clear cart for user: {}", userDetails.getUsername());
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            if (user == null) {
                logger.error("User not found in database");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            // Clear all items from the user's cart.
            shoppingCartService.clearCart(user);
            logger.info("Cart cleared successfully for user: {}", user.getUsername());
            return ResponseEntity.ok("Cart cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing cart for user: " + userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to clear cart: " + e.getMessage());
        }
    }

    /**
     * Retrieve the total cost of items in the user's shopping cart.
     * Returns the total as a JSON response for dynamic updates in the UI.
     */
    @GetMapping("/total")
    @ResponseBody
    public ResponseEntity<Double> getCartTotal(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        BigDecimal total = shoppingCartService.calculateCartTotal(user);
        return ResponseEntity.ok(total.doubleValue());
    }

    /**
     * Update the quantity of a specific item in the user's shopping cart.
     * Ensures that quantities are valid and updates the cart accordingly.
     */
    @PostMapping("/update-quantity")
    @ResponseBody
    public ResponseEntity<String> updateQuantity(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("recordId") Long recordId,
            @RequestParam("quantity") int quantity) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            User user = userService.findByUsername(userDetails.getUsername());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            if (quantity < 1) {
                return ResponseEntity.badRequest().body("Quantity must be at least 1");
            }

            shoppingCartService.updateCartItemQuantity(user, recordId, quantity);
            return ResponseEntity.ok("Quantity updated successfully");
        } catch (Exception e) {
            logger.error("Error updating quantity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update quantity: " + e.getMessage());
        }
    }

    /**
     * Retrieve the total number of items in the user's shopping cart.
     * Used for displaying the cart item count in the UI dynamically.
     */
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Integer> getCartItemCount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(0); // Return 0 if the user is not authenticated.
        }
        User user = userService.findByUsername(userDetails.getUsername());
        int count = shoppingCartService.getCartItemCount(user);
        return ResponseEntity.ok(count);
    }
}
