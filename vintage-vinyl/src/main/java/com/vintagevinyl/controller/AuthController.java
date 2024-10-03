package com.vintagevinyl.controller;

import com.vintagevinyl.exception.OrderNotFoundException;
import com.vintagevinyl.model.User;
import com.vintagevinyl.model.Wishlist;
import com.vintagevinyl.service.RecordService;
import com.vintagevinyl.service.UserService;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.model.Order;
import com.vintagevinyl.service.OrderService;
import com.vintagevinyl.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import com.vintagevinyl.exception.UserAlreadyExistsException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.GrantedAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AuthenticationManager authenticationManager;

    // 直接重定向到 dashboard
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";  // Redirect to dashboard directly
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.registerNewUser(user);
            redirectAttributes.addFlashAttribute("message", "Registration successful. Please log in.");
            return "redirect:/login";
        } catch (UserAlreadyExistsException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            User user = userService.findByUsername(username);
            model.addAttribute("username", username);
            model.addAttribute("userRoles", auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));

            // 获取用户的最近订单
            List<Order> recentOrders = orderService.getRecentOrdersForUser(user, 5);
            model.addAttribute("recentOrders", recentOrders);
        }
        return "dashboard";
    }

    @GetMapping("/orders")
    public String viewOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<Order> orders = orderService.getAllOrdersForUser(user);
        model.addAttribute("orders", orders);
        return "orders";
    }

    @GetMapping("/account")
    public String manageAccount(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(auth.getName());
        model.addAttribute("user", user);
        return "account";
    }

    @PostMapping("/account")
    public String updateAccount(@Valid @ModelAttribute("user") User user, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Received update request for user: {}", user);
        if (result.hasErrors()) {
            logger.warn("Validation errors occurred while updating account for user: {}", user.getUsername());
            result.getAllErrors().forEach(error -> logger.warn("Validation error: {}", error.getDefaultMessage()));
            model.addAttribute("error", "Please correct the errors and try again.");
            return "account";
        }
        try {
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("message", "Account updated successfully.");
            logger.info("Account updated successfully for user: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Error updating account for user: {}", user.getUsername(), e);
            model.addAttribute("error", "Failed to update account: " + e.getMessage());
            return "account";
        }
        return "redirect:/account";
    }

    @Autowired
    private WishlistService wishlistService;

    @GetMapping("/wishlist")
    public String viewWishlist(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByUsername(auth.getName());
            Wishlist wishlist = wishlistService.getWishlistForUser(user);
            logger.info("Fetched wishlist for user: {}, Wishlist items: {}", user.getUsername(), wishlist.getItems());
            model.addAttribute("wishlist", wishlist);
            return "wishlist";
        } catch (Exception e) {
            logger.error("Error viewing wishlist", e);
            model.addAttribute("error", "An error occurred while loading your wishlist.");
            return "error";
        }
    }

    @PostMapping("/wishlist/add")
    public String addToWishlist(@RequestParam Long recordId, RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByUsername(auth.getName());
            Record record = recordService.getRecordById(recordId);
            wishlistService.addToWishlist(user, record);
            redirectAttributes.addFlashAttribute("message", "Item added to wishlist successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add item to wishlist: " + e.getMessage());
        }
        return "redirect:/records";
    }

    @PostMapping("/wishlist/remove")
    public String removeFromWishlist(@RequestParam int itemIndex, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(auth.getName());
        wishlistService.removeItemFromWishlist(user, itemIndex);
        redirectAttributes.addFlashAttribute("message", "Item removed from wishlist.");
        return "redirect:/wishlist";
    }
}
