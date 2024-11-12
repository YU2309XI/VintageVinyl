package com.vintagevinyl.service;

import com.vintagevinyl.exception.UserAlreadyExistsException;
import com.vintagevinyl.model.User;
import com.vintagevinyl.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Initialize test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("tester");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.addRole("ROLE_USER");

        // Initialize admin user
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("encoded_password");
        adminUser.setEnabled(true);
        adminUser.setAccountNonLocked(true);
        adminUser.addRole("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should load user by username successfully")
    void loadUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userService.loadUserByUsername("tester");

        // Then
        assertNotNull(result);
        assertEquals("tester", result.getUsername());
        verify(userRepository).findByUsername("tester");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user not found")
    void loadUserByUsername_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("nonexistent"));
    }

    @Test
    @DisplayName("Should throw DisabledException when user is disabled")
    void loadUserByUsername_UserDisabled() {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(DisabledException.class,
                () -> userService.loadUserByUsername("tester"));
    }

    @Test
    @DisplayName("Should throw LockedException when user is locked")
    void loadUserByUsername_UserLocked() {
        // Given
        testUser.setAccountNonLocked(false);
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(LockedException.class,
                () -> userService.loadUserByUsername("tester"));
    }

    @Test
    @DisplayName("Should register new user successfully")
    void registerNewUser_Success() {
        // Given
        User newUser = new User();
        newUser.setUsername("new user");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password");

        when(userRepository.findByUsername("new user")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        User result = userService.registerNewUser(newUser);

        // Then
        assertNotNull(result);
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonLocked());
        assertTrue(result.getRoles().contains("ROLE_USER"));
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void registerNewUser_UsernameExists() {
        // Given
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(testUser));

        User newUser = new User();
        newUser.setUsername("tester");
        newUser.setEmail("new@example.com");

        // When & Then
        assertThrows(UserAlreadyExistsException.class,
                () -> userService.registerNewUser(newUser));
    }

    @Test
    @DisplayName("Should activate user successfully")
    void activateUser_Success() {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.activateUser(1L, "admin");

        // Then
        assertTrue(result.isEnabled());
        assertEquals("admin", result.getLastModifiedBy());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should deactivate user successfully")
    void deactivateUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.deactivateUser(1L, "admin");

        // Then
        assertFalse(result.isEnabled());
        assertEquals("admin", result.getLastModifiedBy());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should not deactivate last admin user")
    void deactivateUser_LastAdmin() {
        // Given
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRolesContainingAndEnabled("ROLE_ADMIN", true)).thenReturn(1L);

        // When & Then
        assertThrows(IllegalStateException.class,
                () -> userService.deactivateUser(2L, "admin"));
    }

    @Test
    @DisplayName("Should lock user successfully")
    void lockUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.lockUser(1L, "admin");

        // Then
        assertFalse(result.isAccountNonLocked());
        assertEquals("admin", result.getLastModifiedBy());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should not lock last admin user")
    void lockUser_LastAdmin() {
        // Given
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRolesContainingAndEnabled("ROLE_ADMIN", true)).thenReturn(1L);

        // When & Then
        assertThrows(IllegalStateException.class,
                () -> userService.lockUser(2L, "admin"));
    }

    @Test
    @DisplayName("Should unlock user successfully")
    void unlockUser_Success() {
        // Given
        testUser.setAccountNonLocked(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.unlockUser(1L, "admin");

        // Then
        assertTrue(result.isAccountNonLocked());
        assertEquals("admin", result.getLastModifiedBy());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should update user successfully")
    void updateUser_Success() {
        // Given
        User updatedUser = new User();
        updatedUser.setEmail("updated@example.com");
        updatedUser.setPassword("password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUser(1L, updatedUser, "admin");

        // Then
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("admin", result.getLastModifiedBy());
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should change password successfully")
    void changeUserPassword_Success() {
        // Given
        when(passwordEncoder.matches("currentPassword", "encoded_password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encoded_new_password");

        // When
        userService.changeUserPassword(testUser, "currentPassword", "newPassword");

        // Then
        assertEquals("encoded_new_password", testUser.getPassword());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when current password is incorrect")
    void changeUserPassword_IncorrectCurrentPassword() {
        // Given
        when(passwordEncoder.matches("wrongPassword", "encoded_password")).thenReturn(false);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> userService.changeUserPassword(testUser, "wrongPassword", "newPassword"));
    }

    @Test
    @DisplayName("Should find active users")
    void findActiveUsers_Success() {
        // Given
        List<User> activeUsers = Arrays.asList(testUser, adminUser);
        when(userRepository.findByEnabled(true)).thenReturn(activeUsers);

        // When
        List<User> result = userService.findActiveUsers();

        // Then
        assertEquals(2, result.size());
        verify(userRepository).findByEnabled(true);
    }

    @Test
    @DisplayName("Should find locked users")
    void findLockedUsers_Success() {
        // Given
        testUser.setAccountNonLocked(false);
        List<User> lockedUsers = List.of(testUser);
        when(userRepository.findByAccountNonLocked(false)).thenReturn(lockedUsers);

        // When
        List<User> result = userService.findLockedUsers();

        // Then
        assertEquals(1, result.size());
        verify(userRepository).findByAccountNonLocked(false);
    }

    @Test
    @DisplayName("Should check if user is last active admin")
    void isLastActiveAdmin_True() {
        // Given
        when(userRepository.countByRolesContainingAndEnabled("ROLE_ADMIN", true)).thenReturn(1L);

        // When
        boolean result = userService.isLastActiveAdmin(adminUser);

        // Then
        assertTrue(result);
    }
}