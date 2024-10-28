package com.vintagevinyl.controller;

import com.vintagevinyl.exception.OrderNotFoundException;
import com.vintagevinyl.model.Order;
import com.vintagevinyl.model.ShoppingCart;
import com.vintagevinyl.model.User;
import com.vintagevinyl.service.ShoppingCartService;
import com.vintagevinyl.service.UserService;
import com.vintagevinyl.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class OrderController {

    @Autowired
    private UserService userService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest request;
    @GetMapping("/checkout")
    @Transactional
    public String showCheckoutForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        ShoppingCart cart = shoppingCartService.getCart(user);
        cart.getItems().size();
        model.addAttribute("cart", cart);
        return "checkout";
    }

    @PostMapping("/checkout")
    @Transactional
    public String processCheckout(@AuthenticationPrincipal UserDetails userDetails,
                                  @RequestParam("shippingAddress") String shippingAddress,
                                  @RequestParam("paymentMethod") String paymentMethod) {
        System.out.println("Processing checkout...");
        System.out.println("User: " + userDetails.getUsername());
        System.out.println("Shipping Address: " + shippingAddress);
        System.out.println("Payment Method: " + paymentMethod);

        User user = userService.findByUsername(userDetails.getUsername());
        ShoppingCart cart = shoppingCartService.getCart(user);

        try {
            Long orderId = orderService.createOrder(user, cart, shippingAddress, paymentMethod);
            System.out.println("Order created successfully. Order ID: " + orderId);
            shoppingCartService.clearCart(user);
            return "redirect:/orders/" + orderId;
        } catch (Exception e) {
            System.out.println("Error creating order: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/checkout?error";
        }
    }

    @GetMapping("/orders/{orderId}")
    public String viewOrderDetails(@PathVariable("orderId") Long orderId,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Model model) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            Order order;

            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (isAdmin) {
                order = orderService.getOrderById(orderId);
            } else {
                order = orderService.getOrderByIdAndUser(orderId, user);
            }

            model.addAttribute("order", order);
            return "order-details";
        } catch (OrderNotFoundException e) {
            model.addAttribute("error", "Order not found");
            return "redirect:/orders";
        }
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewAllOrders(Model model) {
        List<Order> allOrders = orderService.getAllOrders();
        model.addAttribute("orders", allOrders);
        return "admin-orders";
    }


    @GetMapping("/admin/orders/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewAdminOrderDetails(@PathVariable("orderId") Long orderId, Model model) {
        try {
            Order order = orderService.getOrderById(orderId);
            model.addAttribute("order", order);
            return "order-details";
        } catch (OrderNotFoundException e) {
            return "redirect:/error";
        }
    }

    @PostMapping("/admin/orders/{orderId}/update-status")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateOrderStatus(@PathVariable("orderId") Long orderId,
                                    @RequestParam("status") String status,
                                    RedirectAttributes redirectAttributes,
                                    HttpServletRequest request) {
        try {
            orderService.updateOrderStatus(orderId, status);
            redirectAttributes.addFlashAttribute("message", "Order status updated successfully to " + status);
            return "redirect:/admin/orders";        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating order status: " + e.getMessage());
            return "redirect:/orders/" + orderId;
        }
    }

    @PostMapping("/orders/{orderId}/cancel")
    public String cancelOrder(@PathVariable("orderId") Long orderId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            orderService.cancelOrder(orderId, user);
            redirectAttributes.addFlashAttribute("message", "Order cancelled successfully.");

            if (userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/admin/orders";
            }
            return "redirect:/orders";
        } catch (OrderNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Order not found");
            return "redirect:/orders";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/orders/" + orderId;
        }
    }


    @PostMapping("/admin/orders/{orderId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteOrder(@PathVariable("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        try {
            orderService.deleteOrder(orderId);
            redirectAttributes.addFlashAttribute("message", "Order deleted successfully.");
        } catch (OrderNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Order not found.");
        }
        return "redirect:/admin/orders";
    }

}