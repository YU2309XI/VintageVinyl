package com.vintagevinyl.service;

import com.vintagevinyl.model.User;
import com.vintagevinyl.repository.UserRepository;
import com.vintagevinyl.exception.UserAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService implements UserDetailsService, GenericService<User, Long> {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Attempting to load user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        if (!user.isEnabled()) {
            logger.error("User account is disabled: {}", username);
            throw new DisabledException("User account is disabled");
        }

        if (!user.isAccountNonLocked()) {
            logger.error("User account is locked: {}", username);
            throw new LockedException("User account is locked");
        }

        logger.info("Successfully loaded user: {}, enabled: {}, locked: {}, roles: {}",
                username, user.isEnabled(), user.isAccountNonLocked(), user.getRoles());
        return user;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional
    public User registerNewUser(User user) {
        logger.info("Attempting to register new user: {}", user.getUsername());

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            logger.warn("Username already exists: {}", user.getUsername());
            throw new UserAlreadyExistsException("Username already exists");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            logger.warn("Email already exists: {}", user.getEmail());
            throw new UserAlreadyExistsException("Email already exists");
        }

        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.addRole("ROLE_USER");

        User savedUser = userRepository.save(user);
        logger.info("Successfully registered new user: {}, roles: {}",
                savedUser.getUsername(), savedUser.getRoles());
        return savedUser;
    }

    @Transactional
    public User activateUser(Long userId, String modifiedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        user.setEnabled(true);
        user.setLastModifiedBy(modifiedBy);
        return userRepository.save(user);
    }

    @Transactional
    public User deactivateUser(Long userId, String modifiedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (user.getRoles().contains("ROLE_ADMIN")) {
            long activeAdminCount = userRepository.countByRolesContainingAndEnabled("ROLE_ADMIN", true);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot deactivate the last admin user");
            }
        }

        user.setEnabled(false);
        user.setLastModifiedBy(modifiedBy);
        return userRepository.save(user);
    }

    @Transactional
    public User lockUser(Long userId, String modifiedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (user.getRoles().contains("ROLE_ADMIN")) {
            long activeAdminCount = userRepository.countByRolesContainingAndEnabled("ROLE_ADMIN", true);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot lock the last admin user");
            }
        }

        user.setAccountNonLocked(false);
        user.setLastModifiedBy(modifiedBy);
        return userRepository.save(user);
    }

    @Transactional
    public User unlockUser(Long userId, String modifiedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        user.setAccountNonLocked(true);
        user.setLastModifiedBy(modifiedBy);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long userId, User updatedUser, String modifiedBy) {
        logger.info("Updating user with ID: {}", userId);
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (!existingUser.getEmail().equals(updatedUser.getEmail()) &&
                userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setLastModifiedBy(modifiedBy);

        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        User savedUser = userRepository.save(existingUser);
        logger.info("User updated successfully: {}", savedUser.getUsername());
        return savedUser;
    }

    @Transactional
    public User setAdminRole(String username) {
        User user = findByUsername(username);
        user.addRole("ROLE_ADMIN");
        return userRepository.save(user);
    }

    @Transactional
    public void changeUserPassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        if (user.getRoles().contains("ROLE_ADMIN")) {
            long adminCount = userRepository.countByRolesContainingAndEnabled("ROLE_ADMIN", true);
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot delete the last admin user");
            }
        }

        userRepository.deleteById(id);
        logger.info("User deleted successfully: {}", user.getUsername());
    }

    public List<User> findActiveUsers() {
        return userRepository.findByEnabled(true);
    }

    public List<User> findLockedUsers() {
        return userRepository.findByAccountNonLocked(false);
    }

    public boolean isLastActiveAdmin(User user) {
        return user.getRoles().contains("ROLE_ADMIN") &&
                userRepository.countByRolesContainingAndEnabled("ROLE_ADMIN", true) <= 1;
    }
}