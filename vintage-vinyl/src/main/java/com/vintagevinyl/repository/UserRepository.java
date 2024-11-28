package com.vintagevinyl.repository;

import com.vintagevinyl.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

/**
 * Repository interface for managing User entities.
 *
 * This interface extends JpaRepository, providing CRUD operations
 * and custom query methods for the User entity.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a User by their username.
     *
     * @param username the username of the User
     * @return an Optional containing the User, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a User by their email address.
     *
     * @param email the email of the User
     * @return an Optional containing the User, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Retrieve all Users with the specified enabled status.
     *
     * @param enabled the enabled status to filter by
     * @return a List of Users with the given enabled status
     */
    List<User> findByEnabled(boolean enabled);

    /**
     * Retrieve all Users with the specified account non-locked status.
     *
     * @param accountNonLocked the account non-locked status to filter by
     * @return a List of Users with the given account non-locked status
     */
    List<User> findByAccountNonLocked(boolean accountNonLocked);

    /**
     * Retrieve all Users whose roles contain the specified role.
     *
     * @param role the role to filter by
     * @return a List of Users whose roles contain the given role
     */
    List<User> findByRolesContaining(String role);

    /**
     * Retrieve all Users with both the specified enabled and account non-locked statuses.
     *
     * @param enabled the enabled status to filter by
     * @param accountNonLocked the account non-locked status to filter by
     * @return a List of Users matching the given enabled and account non-locked statuses
     */
    List<User> findByEnabledAndAccountNonLocked(boolean enabled, boolean accountNonLocked);

    /**
     * Count the number of Users with the specified role and enabled status.
     *
     * @param role the role to filter by
     * @param enabled the enabled status to filter by
     * @return the count of Users matching the given criteria
     */
    long countByRolesContainingAndEnabled(String role, boolean enabled);
}
