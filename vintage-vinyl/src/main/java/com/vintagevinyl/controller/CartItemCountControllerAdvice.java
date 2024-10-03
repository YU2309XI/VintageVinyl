package com.vintagevinyl.controller;

import com.vintagevinyl.model.User;
import com.vintagevinyl.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CartItemCountControllerAdvice {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @ModelAttribute("cartItemCount")
    public Integer cartItemCount(@AuthenticationPrincipal User user) {
        if (user != null) {
            return shoppingCartService.getCartItemCount(user);
        }
        return 0;
    }
}