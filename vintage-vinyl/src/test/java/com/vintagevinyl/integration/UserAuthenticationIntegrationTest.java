package com.vintagevinyl.integration;

import com.vintagevinyl.model.User;
import com.vintagevinyl.service.UserService;
import com.vintagevinyl.exception.UserAlreadyExistsException;
import com.vintagevinyl.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for user registration and authentication flows
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserAuthenticationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Test successful user registration and login
     */
    @Test
    void registerAndLoginFlow() {
        // Create new user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123");

        // Register user
        User savedUser = userService.registerNewUser(user);

        // Verify user was saved
        assertNotNull(savedUser.getId(), "User should have an ID after registration");
        assertTrue(savedUser.isEnabled(), "User should be enabled by default");
        assertTrue(savedUser.isAccountNonLocked(), "User account should be non-locked by default");

        // Verify password is encrypted
        assertTrue(passwordEncoder.matches("password123", savedUser.getPassword()),
                "Password should be encrypted");

        // Verify user can be loaded
        UserDetails userDetails = userService.loadUserByUsername("testuser");
        assertNotNull(userDetails, "Should be able to load user by username");
        assertEquals("testuser", userDetails.getUsername(), "Username should match");
        assertTrue(userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_USER")),
                "User should have ROLE_USER authority");
    }

    /**
     * Test registration with duplicate username
     */
    @Test
    void duplicateUsernameRegistration() {
        // Register first user
        User user1 = new User();
        user1.setUsername("testuser");
        user1.setEmail("test1@example.com");
        user1.setPassword("password123");
        userService.registerNewUser(user1);

        // Try to register second user with same username
        User user2 = new User();
        user2.setUsername("testuser");
        user2.setEmail("test2@example.com");
        user2.setPassword("password456");

        Exception exception = assertThrows(UserAlreadyExistsException.class, () ->
                        userService.registerNewUser(user2),
                "Should throw exception when registering duplicate username");

        assertTrue(exception.getMessage().contains("Username already exists"),
                "Exception message should indicate duplicate username");
    }

    /**
     * Test registration with duplicate email
     */
    @Test
    void duplicateEmailRegistration() {
        // Register first user
        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("test@example.com");
        user1.setPassword("password123");
        userService.registerNewUser(user1);

        // Try to register second user with same email
        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("test@example.com");
        user2.setPassword("password456");

        Exception exception = assertThrows(UserAlreadyExistsException.class, () ->
                        userService.registerNewUser(user2),
                "Should throw exception when registering duplicate email");

        assertTrue(exception.getMessage().contains("Email already exists"),
                "Exception message should indicate duplicate email");
    }

    /**
     * Test loading non-existent user
     */
    @Test
    void loadNonExistentUser() {
        assertThrows(UsernameNotFoundException.class, () ->
                        userService.loadUserByUsername("nonexistentuser"),
                "Should throw exception when loading non-existent user");
    }

    /**
     * Test user role assignment
     */
    @Test
    void userRoleAssignment() {
        // Register user
        User user = new User();
        user.setUsername("testadmin");
        user.setEmail("admin@example.com");
        user.setPassword("password123");
        User savedUser = userService.registerNewUser(user);

        // Set admin role
        userService.setAdminRole(savedUser.getUsername());

        // Verify roles
        User updatedUser = userRepository.findByUsername(savedUser.getUsername())
                .orElseThrow();
        assertTrue(updatedUser.getRoles().contains("ROLE_USER"),
                "User should retain ROLE_USER");
        assertTrue(updatedUser.getRoles().contains("ROLE_ADMIN"),
                "User should have ROLE_ADMIN");
    }
}