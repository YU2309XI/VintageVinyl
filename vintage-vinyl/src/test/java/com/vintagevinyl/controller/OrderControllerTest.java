package com.vintagevinyl.controller;

import com.vintagevinyl.exception.OrderNotFoundException;
import com.vintagevinyl.model.Order;
import com.vintagevinyl.model.OrderItem;
import com.vintagevinyl.model.ShoppingCart;
import com.vintagevinyl.model.User;
import com.vintagevinyl.service.OrderService;
import com.vintagevinyl.service.ShoppingCartService;
import com.vintagevinyl.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest // Full application context
@AutoConfigureMockMvc // Enable MockMvc for HTTP testing
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc; // This helps us simulate HTTP requests

    @MockBean
    private OrderService orderService;

    @MockBean
    private UserService userService;

    @MockBean
    private ShoppingCartService shoppingCartService;

    private User testUser;
    private Order testOrder;
    private ShoppingCart testCart;

    @BeforeEach
    void setUp() {
        // Set up test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        // Set up test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setOrderDate(new Date());
        testOrder.setStatus("PENDING");
        testOrder.setTotal(new BigDecimal("99.99"));
        testOrder.setShippingAddress("123 Test St");
        testOrder.setPaymentMethod("CREDIT_CARD");
        testOrder.setItems(new ArrayList<>());


        // Set up test cart
        testCart = new ShoppingCart();
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>());
    }

    @Nested
    @DisplayName("Checkout Process Tests")
    class CheckoutTests {
        /**
         * Test case for showing the checkout form.
         */
        @Test
        @WithMockUser(username = "testUser")  // This simulates a logged-in user
        @DisplayName("Should show checkout form")
        void showCheckoutForm_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(shoppingCartService.getCart(testUser)).thenReturn(testCart);

            mockMvc.perform(get("/checkout")) // Sending a POST request to /checkout
                    .andExpect(status().isOk())
                    .andExpect(view().name("checkout"))
                    .andExpect(model().attributeExists("cart"));
        }

        /**
         * Test case for successfully processing a checkout.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should process checkout successfully")
        void processCheckout_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(shoppingCartService.getCart(testUser)).thenReturn(testCart);
            when(orderService.createOrder(eq(testUser), eq(testCart), anyString(), anyString()))
                    .thenReturn(1L);

            mockMvc.perform(post("/checkout")
                            .with(csrf()) // Adding CSRF token for security
                            .param("shippingAddress", "123 Test St")
                            .param("paymentMethod", "CREDIT_CARD"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/orders/1"));

            verify(shoppingCartService).clearCart(testUser);
        }

        /**
         * Test case for handling errors during checkout.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should handle checkout error")
        void processCheckout_Error() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(shoppingCartService.getCart(testUser)).thenReturn(testCart);
            when(orderService.createOrder(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Error processing order"));

            mockMvc.perform(post("/checkout")
                            .with(csrf())
                            .param("shippingAddress", "123 Test St")
                            .param("paymentMethod", "CREDIT_CARD"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/checkout?error"));
        }
    }

    @Nested
    @DisplayName("Order View Tests")
    class OrderViewTests {
        /**
         * Test case for viewing order details as a user.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should show order details to user")
        void viewOrderDetails_UserSuccess() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(orderService.getOrderByIdAndUser(1L, testUser)).thenReturn(testOrder);

            mockMvc.perform(get("/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("order-details"))
                    .andExpect(model().attributeExists("order"));
        }

        /**
         * Test case for viewing order details as an admin.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should show order details to admin")
        void viewOrderDetails_AdminSuccess() throws Exception {
            when(orderService.getOrderById(1L)).thenReturn(testOrder);

            mockMvc.perform(get("/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("order-details"))
                    .andExpect(model().attributeExists("order"));
        }

        /**
         * Test case for handling 'order not found' scenario.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should handle order not found")
        void viewOrderDetails_NotFound() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(orderService.getOrderByIdAndUser(1L, testUser))
                    .thenThrow(new OrderNotFoundException("Order not found"));

            mockMvc.perform(get("/orders/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/orders"));
        }
    }

    @Nested
    @DisplayName("Admin Order Management Tests")
    class AdminOrderTests {

        /**
         * Test case for listing all orders for admin.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should list all orders for admin")
        void viewAllOrders_Success() throws Exception {
            List<Order> orders = Arrays.asList(testOrder);
            when(orderService.getAllOrders()).thenReturn(orders);

            mockMvc.perform(get("/admin/orders"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-orders"))
                    .andExpect(model().attributeExists("orders"));
        }

        /**
         * Test case for viewing order details in admin view.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should show order details in admin view")
        void viewAdminOrderDetails_Success() throws Exception {
            when(orderService.getOrderById(1L)).thenReturn(testOrder);

            mockMvc.perform(get("/admin/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("order-details"))
                    .andExpect(model().attributeExists("order"));
        }

        /**
         * Test case for updating the status of an order.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update order status")
        void updateOrderStatus_Success() throws Exception {
            doNothing().when(orderService).updateOrderStatus(1L, "SHIPPED");

            mockMvc.perform(post("/admin/orders/1/update-status")
                            .with(csrf())
                            .param("status", "SHIPPED"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/orders"))
                    .andExpect(flash().attributeExists("message"));
        }

        /**
         * Test case for handling errors during order status updates.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle order status update error")
        void updateOrderStatus_Error() throws Exception {
            doThrow(new RuntimeException("Update failed"))
                    .when(orderService).updateOrderStatus(1L, "INVALID_STATUS");

            mockMvc.perform(post("/admin/orders/1/update-status")
                            .with(csrf())
                            .param("status", "INVALID_STATUS"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/orders/1"))
                    .andExpect(flash().attributeExists("error"));
        }

        /**
         * Test case for deleting an order as an admin.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete order")
        void deleteOrder_Success() throws Exception {
            doNothing().when(orderService).deleteOrder(1L);

            mockMvc.perform(post("/admin/orders/1/delete")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/orders"))
                    .andExpect(flash().attributeExists("message"));
        }
    }

    @Nested
    @DisplayName("Order Management Tests")
    class OrderManagementTests {

        /**
         * Test case for canceling an order as a user.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should cancel order as user")
        void cancelOrder_UserSuccess() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            doNothing().when(orderService).cancelOrder(1L, testUser);

            mockMvc.perform(post("/orders/1/cancel")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/orders"))
                    .andExpect(flash().attributeExists("message"));
        }

        /**
         * Test case for canceling an order as an admin.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should cancel order as admin")
        void cancelOrder_AdminSuccess() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            doNothing().when(orderService).cancelOrder(1L, testUser);

            mockMvc.perform(post("/orders/1/cancel")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/orders"))
                    .andExpect(flash().attributeExists("message"));
        }

        /**
         * Test case for handling errors during order cancellation.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should handle order cancellation error")
        void cancelOrder_Error() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            doThrow(new IllegalStateException("Cannot cancel shipped order"))
                    .when(orderService).cancelOrder(1L, testUser);

            mockMvc.perform(post("/orders/1/cancel")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/orders/1"))
                    .andExpect(flash().attributeExists("error"));
        }
    }
}