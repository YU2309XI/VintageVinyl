package com.vintagevinyl.controller;

import com.vintagevinyl.model.ShoppingCart;
import com.vintagevinyl.model.User;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ShoppingCartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ShoppingCartService shoppingCartService;

    private User testUser;
    private ShoppingCart testCart;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        testCart = new ShoppingCart();
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should redirect unauthenticated user to login")
    void cartAccess_Unauthenticated() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Nested
    @DisplayName("Cart Operation Tests")
    class CartOperationTests {

        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should show cart contents")
        void viewCart_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(shoppingCartService.getCart(testUser)).thenReturn(testCart);

            mockMvc.perform(get("/cart"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("cart"))
                    .andExpect(model().attributeExists("cart"));
        }

        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should add item to cart")
        void addToCart_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            doNothing().when(shoppingCartService).addItemToCart(eq(testUser), eq(1L), eq(1));

            mockMvc.perform(post("/cart/add")
                            .with(csrf())
                            .param("recordId", "1")
                            .param("quantity", "1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/wishlist"))
                    .andExpect(flash().attributeExists("message"));
        }

        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should remove item from cart")
        void removeFromCart_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            doNothing().when(shoppingCartService).removeItemFromCart(testUser, 1L);

            mockMvc.perform(post("/cart/remove")
                            .with(csrf())
                            .param("recordId", "1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/cart"))
                    .andExpect(flash().attributeExists("message"));
        }

        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should clear cart")
        void clearCart_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            doNothing().when(shoppingCartService).clearCart(testUser);

            mockMvc.perform(post("/cart/clear")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Cart cleared successfully"));
        }

        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should get cart total")
        void getCartTotal_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(shoppingCartService.calculateCartTotal(testUser)).thenReturn(new BigDecimal("99.99"));

            mockMvc.perform(get("/cart/total"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("99.99"));
        }

        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should update item quantity")
        void updateQuantity_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            doNothing().when(shoppingCartService).updateCartItemQuantity(testUser, 1L, 2);

            mockMvc.perform(post("/cart/update-quantity")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("recordId", "1")
                            .param("quantity", "2"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Quantity updated successfully"));
        }

        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should get cart item count")
        void getCartItemCount_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(shoppingCartService.getCartItemCount(testUser)).thenReturn(5);

            mockMvc.perform(get("/cart/count"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5"));
        }
    }

    @Nested
    @DisplayName("Cart Error Handling Tests")
    class CartErrorTests {

        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should handle user not found")
        void cartOperation_UserNotFound() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(null);

            mockMvc.perform(get("/cart"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }

        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should handle invalid quantity")
        void updateQuantity_InvalidQuantity() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);

            mockMvc.perform(post("/cart/update-quantity")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("recordId", "1")
                            .param("quantity", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Quantity must be at least 1"));
        }
    }
}