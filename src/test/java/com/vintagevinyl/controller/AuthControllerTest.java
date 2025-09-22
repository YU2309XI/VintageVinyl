package com.vintagevinyl.controller;

import com.vintagevinyl.model.Order;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.model.User;
import com.vintagevinyl.model.Wishlist;
import com.vintagevinyl.exception.UserAlreadyExistsException;
import com.vintagevinyl.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private RecordService recordService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private WishlistService wishlistService;

    private User testUser;
    private Record testRecord;
    private Order testOrder;
    private Wishlist testWishlist;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");

        testRecord = new Record();
        testRecord.setId(1L);
        testRecord.setTitle("Test Record");

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);

        testWishlist = new Wishlist();
        testWishlist.setId(1L);
        testWishlist.setUser(testUser);
        testWishlist.setItems(new ArrayList<>());
    }

    @Nested
    @DisplayName("Public Access Tests")
    class PublicAccessTests {

        /**
         * Test case for accessing the home page.
         */
        @Test
        @WithAnonymousUser
        @DisplayName("Should access home page")
        void home_ShouldRedirectToDashboard() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"));
        }

        /**
         * Test case for displaying the registration form.
         */
        @Test
        @WithAnonymousUser
        @DisplayName("Should show registration form")
        void showRegistrationForm_ShouldDisplayPage() throws Exception {
            mockMvc.perform(get("/register"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("user"));
        }

        /**
         * Test case for successfully registering a new user.
         */
        @Test
        @WithAnonymousUser
        @DisplayName("Should register new user successfully")
        void registerUser_Success() throws Exception {
            when(userService.registerNewUser(any(User.class))).thenReturn(testUser);

            mockMvc.perform(post("/register")
                            .with(csrf())
                            .param("username", "testUser")
                            .param("email", "test@example.com")
                            .param("password", "password"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"))
                    .andExpect(flash().attributeExists("message"));
        }

        /**
         * Test case for handling an already existing user during registration.
         */
        @Test
        @WithAnonymousUser
        @DisplayName("Should handle user already exists during registration")
        void registerUser_UserExists() throws Exception {
            when(userService.registerNewUser(any(User.class)))
                    .thenThrow(new UserAlreadyExistsException("User already exists"));

            mockMvc.perform(post("/register")
                            .with(csrf())
                            .param("username", "testUser")
                            .param("email", "test@example.com")
                            .param("password", "password"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("error"));
        }

        /**
         * Test case for displaying the login form.
         */
        @Test
        @WithAnonymousUser
        @DisplayName("Should show login form")
        void showLoginForm_ShouldDisplayPage() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"));
        }

        /**
         * Test case for displaying the login form with an error message.
         */
        @Test
        @WithAnonymousUser
        @DisplayName("Should show login form with error")
        void showLoginForm_WithError() throws Exception {
            mockMvc.perform(get("/login").param("error", "true"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"))
                    .andExpect(model().attribute("error", "Invalid username or password."));
        }
    }

    @Nested
    @DisplayName("Authenticated User Tests")
    class AuthenticatedUserTests {

        /**
         * Test case for displaying the user dashboard.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should display dashboard")
        void dashboard_ShouldDisplayPage() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            List<Order> recentOrders = Collections.singletonList(testOrder);
            when(orderService.getRecentOrdersForUser(any(User.class), anyInt())).thenReturn(recentOrders);

            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard"))
                    .andExpect(model().attributeExists("username", "userRoles", "recentOrders"));
        }

        /**
         * Test case for viewing user orders.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should display orders")
        void viewOrders_ShouldDisplayPage() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            List<Order> orders = Collections.singletonList(testOrder);
            when(orderService.getAllOrdersForUser(any(User.class))).thenReturn(orders);

            mockMvc.perform(get("/orders"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("orders"))
                    .andExpect(model().attributeExists("orders"));
        }

        /**
         * Test case for managing user account.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should display account management")
        void manageAccount_ShouldDisplayPage() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);

            mockMvc.perform(get("/account"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("account"))
                    .andExpect(model().attributeExists("user"));
        }

        /**
         * Test case for successfully updating user account details.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should update account successfully")
        void updateAccount_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(userService.updateUser(anyLong(), any(User.class), anyString())).thenReturn(testUser);

            mockMvc.perform(post("/account")
                            .with(csrf())
                            .param("email", "newemail@example.com"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"))
                    .andExpect(flash().attributeExists("message"));
        }

        /**
         * Test case for updating user account with password change.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should update account with password change")
        void updateAccount_WithPassword() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(userService.updateUser(anyLong(), any(User.class), anyString())).thenReturn(testUser);
            doNothing().when(userService).changeUserPassword(any(User.class), anyString(), anyString());

            mockMvc.perform(post("/account")
                            .with(csrf())
                            .param("email", "newemail@example.com")
                            .param("currentPassword", "oldpass")
                            .param("newPassword", "newpass")
                            .param("confirmNewPassword", "newpass"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"))
                    .andExpect(flash().attributeExists("message"));
        }
    }

    @Nested
    @DisplayName("Wishlist Tests")
    class WishlistTests {

        /**
         * Test case for viewing the wishlist.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should display wishlist")
        void viewWishlist_ShouldDisplayPage() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(wishlistService.getWishlistForUser(any(User.class))).thenReturn(testWishlist);

            mockMvc.perform(get("/wishlist"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("wishlist"))
                    .andExpect(model().attributeExists("wishlist"));
        }

        /**
         * Test case for adding an item to the wishlist.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should add item to wishlist")
        void addToWishlist_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(recordService.getRecordById(anyLong())).thenReturn(testRecord);
            doNothing().when(wishlistService).addToWishlist(any(User.class), any(Record.class));

            mockMvc.perform(post("/wishlist/add")
                            .with(csrf())
                            .param("recordId", "1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/records"))
                    .andExpect(flash().attributeExists("message"));
        }

        /**
         * Test case for removing an item from the wishlist.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should remove item from wishlist")
        void removeFromWishlist_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            doNothing().when(wishlistService).removeItemFromWishlist(any(User.class), anyInt());

            mockMvc.perform(post("/wishlist/remove")
                            .with(csrf())
                            .param("itemIndex", "0"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/wishlist"))
                    .andExpect(flash().attributeExists("message"));
        }
    }
}
