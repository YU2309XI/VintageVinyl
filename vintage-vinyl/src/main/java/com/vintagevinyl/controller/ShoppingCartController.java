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

@Controller
@RequestMapping("/cart")
public class ShoppingCartController {
    private static final Logger logger = LoggerFactory.getLogger(ShoppingCartController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Transactional
    @GetMapping
    public String viewCart(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            logger.error("User not found in database");
            return "redirect:/login";
        }

        ShoppingCart cart = shoppingCartService.getCart(user);
        cart.getItems().forEach(item -> item.getRecord().getTitle());

        model.addAttribute("cart", cart);
        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam("recordId") Long recordId,
                            @RequestParam(defaultValue = "1") int quantity,
                            RedirectAttributes redirectAttributes) {
        logger.debug("Received add to cart request: recordId={}, quantity={}", recordId, quantity);

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            User user = userService.findByUsername(userDetails.getUsername());
            if (user == null) {
                logger.error("User not found in database");
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/wishlist";
            }

            logger.info("Adding item to cart: userId={}, recordId={}, quantity={}", user.getId(), recordId, quantity);
            shoppingCartService.addItemToCart(user, recordId, quantity);
            logger.info("Item added to cart successfully");
            redirectAttributes.addFlashAttribute("message", "Item added to cart successfully");
        } catch (Exception e) {
            logger.error("Error adding item to cart", e);
            redirectAttributes.addFlashAttribute("error", "Failed to add record to cart: " + e.getMessage());
        }
        return "redirect:/wishlist";
    }

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

    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<String> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Attempting to clear cart for user: {}", userDetails.getUsername());
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            if (user == null) {
                logger.error("User not found in database");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            shoppingCartService.clearCart(user);
            logger.info("Cart cleared successfully for user: {}", user.getUsername());
            return ResponseEntity.ok("Cart cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing cart for user: " + userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to clear cart: " + e.getMessage());
        }
    }

    @GetMapping("/total")
    @ResponseBody
    public ResponseEntity<Double> getCartTotal(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        BigDecimal total = shoppingCartService.calculateCartTotal(user);
        return ResponseEntity.ok(total.doubleValue());
    }

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

    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Integer> getCartItemCount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(0);
        }
        User user = userService.findByUsername(userDetails.getUsername());
        int count = shoppingCartService.getCartItemCount(user);
        return ResponseEntity.ok(count);
    }
}