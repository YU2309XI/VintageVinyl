package com.vintagevinyl.controller;

import com.vintagevinyl.model.User;
import com.vintagevinyl.service.ShoppingCartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartItemCountControllerAdviceTest {

    @Mock
    private ShoppingCartService shoppingCartService;

    @InjectMocks
    private CartItemCountControllerAdvice controllerAdvice;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
    }

    /**
     * Test case for retrieving cart item count for an authenticated user.
     */
    @Test
    @DisplayName("Should return cart item count for authenticated user")
    void cartItemCount_AuthenticatedUser() {
        when(shoppingCartService.getCartItemCount(testUser)).thenReturn(5);

        Integer count = controllerAdvice.cartItemCount(testUser);

        assertEquals(5, count);
    }

    /**
     * Test case for retrieving cart item count for an unauthenticated user.
     */
    @Test
    @DisplayName("Should return zero for unauthenticated user")
    void cartItemCount_UnauthenticatedUser() {
        Integer count = controllerAdvice.cartItemCount(null);

        assertEquals(0, count);
    }
}
