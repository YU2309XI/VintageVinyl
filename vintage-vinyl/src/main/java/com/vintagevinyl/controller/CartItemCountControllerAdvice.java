package com.vintagevinyl.controller;

import com.vintagevinyl.model.User;
import com.vintagevinyl.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice to make the shopping cart item count available as a model attribute.
 * This ensures that the cart item count is included in every view rendered by the application.
 */
@ControllerAdvice
public class CartItemCountControllerAdvice {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * Adds the cart item count to the model for all views.
     *
     * @param user The currently authenticated user, injected by Spring Security.
     * @return The total number of items in the user's shopping cart, or 0 if the user is not authenticated.
     */
    @ModelAttribute("cartItemCount")
    public Integer cartItemCount(@AuthenticationPrincipal User user) {
        // If the user is authenticated, retrieve the cart item count from the shopping cart service
        if (user != null) {
            return shoppingCartService.getCartItemCount(user);
        }
        // Default to 0 if the user is not authenticated
        return 0;
    }
}
