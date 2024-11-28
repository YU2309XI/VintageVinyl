package com.vintagevinyl.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a user in the system.
 * Implements the UserDetails interface for Spring Security integration.
 * This entity maps to the "users" table in the database and includes fields for user attributes,
 * roles, and associations with shopping carts and orders.
 */
@Data // Lombok annotation to generate getters, setters, equals, hashCode, and toString methods automatically.
@Entity
@Table(name = "users") // Maps this entity to the "users" table in the database.
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Primary key with auto-increment strategy.
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp // Automatically sets the creation timestamp.
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp // Automatically updates the timestamp when the entity is modified.
    private LocalDateTime updatedAt;

    @Column(unique = true, nullable = false, length = 50)
    @NotBlank(message = "Username is required") // Validates that the username is not blank.
    @Size(max = 50, message = "Username must be no more than 50 characters") // Limits the maximum length of the username.
    private String username;

    @Column(nullable = false)
    @NotBlank(message = "Password is required", groups = {CreateUser.class}) // Validation group for user creation.
    private String password;

    @Column(nullable = false)
    private boolean enabled = true; // Indicates if the user account is enabled.

    @Column(nullable = false)
    private boolean accountNonLocked = true; // Indicates if the user account is locked.

    @Column(name = "last_modified_by")
    private String lastModifiedBy; // Tracks the last user who modified this user.

    public interface CreateUser {} // Validation group marker for user creation.

    @Column(nullable = false, unique = true, length = 100)
    @Email(message = "Invalid email format") // Validates that the email is in a proper format.
    @NotBlank(message = "Email is required") // Ensures the email is not blank.
    @Size(max = 100, message = "Email must be no more than 100 characters") // Limits the maximum length of the email.
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role") // Maps the roles of the user to the "user_roles" table.
    private Set<String> roles = new HashSet<>(); // Stores the roles assigned to the user.

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private ShoppingCart shoppingCart; // One-to-one relationship with the ShoppingCart entity.

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // Prevents circular reference during JSON serialization.
    private List<Order> orders = new ArrayList<>(); // One-to-many relationship with the Order entity.

    /**
     * Adds a role to the user.
     * Automatically prefixes the role with "ROLE_" if not already prefixed.
     *
     * @param role The role to add.
     */
    public void addRole(String role) {
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role; // Ensure roles are prefixed with "ROLE_".
        }
        roles.add(role);
    }

    /**
     * Removes a role from the user.
     *
     * @param role The role to remove.
     */
    public void removeRole(String role) {
        roles.remove(role);
    }

    /**
     * Returns the authorities granted to the user.
     * Converts roles into GrantedAuthority objects for Spring Security.
     *
     * @return A collection of GrantedAuthority objects.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(SimpleGrantedAuthority::new) // Map each role to a SimpleGrantedAuthority object.
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Always returns true as account expiration is not managed.
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked; // Reflects the "accountNonLocked" field.
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Always returns true as credential expiration is not managed.
    }

    @Override
    public boolean isEnabled() {
        return enabled; // Reflects the "enabled" field.
    }

    /**
     * Custom toString implementation for better debugging.
     * Includes key fields such as id, username, email, and roles.
     *
     * @return A string representation of the User object.
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                '}';
    }
}
