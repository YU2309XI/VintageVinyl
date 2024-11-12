package com.vintagevinyl.repository;

import com.vintagevinyl.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    List<User> findByEnabled(boolean enabled);
    List<User> findByAccountNonLocked(boolean accountNonLocked);

    List<User> findByRolesContaining(String role);

    List<User> findByEnabledAndAccountNonLocked(boolean enabled, boolean accountNonLocked);

    long countByRolesContainingAndEnabled(String role, boolean enabled);
}