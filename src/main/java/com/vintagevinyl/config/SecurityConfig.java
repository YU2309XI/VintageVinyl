package com.vintagevinyl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configure the security filter chain for the application.
     * This method defines the authorization rules, CSRF handling, and login/logout configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Customize CSRF token handling by adding a request attribute for client-side use.
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
                // Define authorization rules for endpoints
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints that do not require authentication
                        .requestMatchers("/", "/register", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/records").permitAll()

                        // Admin-only endpoints for inventory management
                        .requestMatchers("/inventory",
                                "/inventory/**",
                                "/api/inventory/**",
                                "/api/records/*/stock",
                                "/api/records/*/threshold",
                                "/api/records/stock/batch",
                                "/api/records/low-stock").hasRole("ADMIN")

                        // Other admin-restricted endpoints
                        .requestMatchers("/records/new", "/records/*/edit", "/records/*/delete").hasRole("ADMIN")
                        .requestMatchers("/admin/**", "/admin/orders/**").hasRole("ADMIN")

                        // Endpoints requiring authentication for specific user actions
                        .requestMatchers("/account").authenticated()
                        .requestMatchers("/wishlist/add").authenticated()
                        .requestMatchers("/orders/**").authenticated()
                        .requestMatchers("/checkout", "/order-confirmation/**").authenticated()
                        .requestMatchers("/cart/**").authenticated()

                        // Default rule: all other requests require authentication
                        .anyRequest().authenticated())
                // Configure login page and behavior after successful or failed login
                .formLogin(form -> form
                        .loginPage("/login") // Custom login page URL
                        .defaultSuccessUrl("/dashboard", true) // Redirect after successful login
                        .failureUrl("/login?error=true") // Redirect on login failure
                        .permitAll()) // Allow access to the login page for everyone
                // Configure logout behavior
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout") // Redirect after logout
                        .permitAll()) // Allow logout for all users
                // Enable "Remember Me" functionality for persistent sessions
                .rememberMe(rememberMe -> rememberMe
                        .key("uniqueAndSecret") // Key for generating persistent tokens
                        .tokenValiditySeconds(86400)) // Token expiration in seconds (24 hours)
                // CSRF protection configuration
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(requestHandler) // Use custom CSRF request handler
                        // Exclude specific endpoints from CSRF protection
                        .ignoringRequestMatchers("/cart/**",
                                "/checkout",
                                "/api/records/*/stock",
                                "/api/records/*/threshold",
                                "/api/records/stock/batch"));

        return http.build(); // Build the security filter chain
    }

    /**
     * Configure the AuthenticationManager.
     * This bean enables the application to use Spring Security's authentication mechanisms.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        // Retrieve the authentication manager from the default configuration
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configure the PasswordEncoder.
     * BCryptPasswordEncoder is used for securely hashing passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt is a strong hashing algorithm recommended for storing passwords securely.
        return new BCryptPasswordEncoder();
    }
}
