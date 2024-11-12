package com.vintagevinyl.repository;

import com.vintagevinyl.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = createTestUser(
                "testuser",
                "test@example.com",
                true,
                true
        );
        testUser.addRole("USER");
    }

    private User createTestUser(String username, String email,
                                boolean enabled, boolean accountNonLocked) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        user.setEnabled(enabled);
        user.setAccountNonLocked(accountNonLocked);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    @DisplayName("Should save and retrieve a user")
    void shouldSaveAndRetrieveUser() {
        // Arrange & Act
        User savedUser = userRepository.save(testUser);
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Assert
        assertThat(foundUser)
                .isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user.getUsername()).isEqualTo(testUser.getUsername());
                    assertThat(user.getEmail()).isEqualTo(testUser.getEmail());
                    assertThat(user.isEnabled()).isTrue();
                    assertThat(user.isAccountNonLocked()).isTrue();
                });
    }

    @Test
    @DisplayName("Should find user by username")
    void shouldFindByUsername() {
        // Arrange
        userRepository.save(testUser);

        // Act
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        // Assert
        assertThat(foundUser)
                .isPresent()
                .hasValueSatisfying(user ->
                        assertThat(user.getUsername()).isEqualTo("testuser")
                );
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindByEmail() {
        // Arrange
        userRepository.save(testUser);

        // Act
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // Assert
        assertThat(foundUser)
                .isPresent()
                .hasValueSatisfying(user ->
                        assertThat(user.getEmail()).isEqualTo("test@example.com")
                );
    }

    @Test
    @DisplayName("Should find users by enabled status")
    void shouldFindByEnabled() {
        // Arrange
        User enabledUser = createTestUser("enabled", "enabled@test.com",
                true, true);
        User disabledUser = createTestUser("disabled", "disabled@test.com",
                false, true);
        userRepository.saveAll(List.of(enabledUser, disabledUser));

        // Act
        List<User> enabledUsers = userRepository.findByEnabled(true);
        List<User> disabledUsers = userRepository.findByEnabled(false);

        // Assert
        assertThat(enabledUsers).hasSize(1)
                .extracting(User::getUsername)
                .containsExactly("enabled");
        assertThat(disabledUsers).hasSize(1)
                .extracting(User::getUsername)
                .containsExactly("disabled");
    }

    @Test
    @DisplayName("Should find users by account lock status")
    void shouldFindByAccountNonLocked() {
        // Arrange
        User unlockedUser = createTestUser("unlocked", "unlocked@test.com",
                true, true);
        User lockedUser = createTestUser("locked", "locked@test.com",
                true, false);
        userRepository.saveAll(List.of(unlockedUser, lockedUser));

        // Act
        List<User> unlockedUsers = userRepository.findByAccountNonLocked(true);
        List<User> lockedUsers = userRepository.findByAccountNonLocked(false);

        // Assert
        assertThat(unlockedUsers).hasSize(1)
                .extracting(User::getUsername)
                .containsExactly("unlocked");
        assertThat(lockedUsers).hasSize(1)
                .extracting(User::getUsername)
                .containsExactly("locked");
    }

    @Test
    @DisplayName("Should find users by role")
    void shouldFindByRole() {
        // Arrange
        User userRole = createTestUser("user1", "user1@test.com",
                true, true);
        userRole.addRole("USER");
        User adminRole = createTestUser("admin1", "admin1@test.com",
                true, true);
        adminRole.addRole("ADMIN");
        userRepository.saveAll(List.of(userRole, adminRole));

        // Act
        List<User> users = userRepository.findByRolesContaining("ROLE_USER");
        List<User> admins = userRepository.findByRolesContaining("ROLE_ADMIN");

        // Assert
        assertThat(users).hasSize(1)
                .extracting(User::getUsername)
                .containsExactly("user1");
        assertThat(admins).hasSize(1)
                .extracting(User::getUsername)
                .containsExactly("admin1");
    }

    @Test
    @DisplayName("Should find users by enabled and lock status")
    void shouldFindByEnabledAndAccountNonLocked() {
        // Arrange
        User activeUser = createTestUser("active", "active@test.com",
                true, true);
        User inactiveUser = createTestUser("inactive", "inactive@test.com",
                false, false);
        userRepository.saveAll(List.of(activeUser, inactiveUser));

        // Act
        List<User> activeUsers = userRepository.findByEnabledAndAccountNonLocked(true, true);
        List<User> inactiveUsers = userRepository.findByEnabledAndAccountNonLocked(false, false);

        // Assert
        assertThat(activeUsers).hasSize(1)
                .extracting(User::getUsername)
                .containsExactly("active");
        assertThat(inactiveUsers).hasSize(1)
                .extracting(User::getUsername)
                .containsExactly("inactive");
    }

    @Test
    @DisplayName("Should count users by role and enabled status")
    void shouldCountByRoleAndEnabled() {
        // Arrange
        User enabledUser = createTestUser("enabled", "enabled@test.com",
                true, true);
        enabledUser.addRole("USER");
        User disabledUser = createTestUser("disabled", "disabled@test.com",
                false, true);
        disabledUser.addRole("USER");
        userRepository.saveAll(List.of(enabledUser, disabledUser));

        // Act
        long enabledCount = userRepository.countByRolesContainingAndEnabled("ROLE_USER", true);
        long disabledCount = userRepository.countByRolesContainingAndEnabled("ROLE_USER", false);

        // Assert
        assertThat(enabledCount).isEqualTo(1);
        assertThat(disabledCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle user role management")
    void shouldManageUserRoles() {
        // Arrange
        testUser.addRole("ADMIN");
        User savedUser = userRepository.save(testUser);

        // Act
        savedUser.removeRole("ROLE_ADMIN");
        User updatedUser = userRepository.save(savedUser);

        // Assert
        assertThat(updatedUser.getRoles()).hasSize(1)
                .containsExactly("ROLE_USER");
    }
}