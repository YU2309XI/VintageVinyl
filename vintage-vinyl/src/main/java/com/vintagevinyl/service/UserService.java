package com.vintagevinyl.service;

import com.vintagevinyl.model.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.Optional;

public interface UserService extends UserDetailsService {
    User registerNewUser(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    void updateUser(User user);
    void deleteUser(Long userId);
}