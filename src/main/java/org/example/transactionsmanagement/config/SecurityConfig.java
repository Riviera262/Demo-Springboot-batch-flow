package org.example.transactionsmanagement.config;

import lombok.RequiredArgsConstructor;
import org.example.transactionsmanagement.security.JwtAuthenticationFilter;
import org.example.transactionsmanagement.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig - Main Security Configuration
 *
 * This is the central security configuration.
 * It sets up:
 * 1. Password encoder (BCrypt)
 * 2. Authentication manager (for login)
 * 3. Security rules (which endpoints are public/protected)
 * 4. JWT filter (check token on every request)
 */
@Configuration
@EnableMethodSecurity  // Enables @PreAuthorize annotations
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PasswordEncoder passwordEncoder;
    /**
     * BEAN 1: Password Encoder
     *
     * Uses BCrypt to hash passwords.
     * BCrypt is one-way - you can't reverse the hash.
     *
     * Example:
     * "password123" → "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
     */

    /**
     * BEAN 2: Authentication Manager
     *
     * Used during login to verify username + password.
     * Spring provides this automatically.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        // Admin > Approver > Uploader
        return RoleHierarchyImpl.fromHierarchy(
                "ROLE_ADMIN > ROLE_APPROVER \n" +
                        "ROLE_ADMIN > ROLE_UPLOADER"
        );
    }

    /**
     * BEAN 3: Authentication Provider
     *
     * Connects:
     * - UserDetailsService (loads user from database)
     * - PasswordEncoder (verifies password hash)
     *
     * Spring Security 6.x requires passing userDetailsService in constructor
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * BEAN 4: Security Filter Chain - MOST IMPORTANT!
     *
     * This defines:
     * 1. Which endpoints are public (no login needed)
     * 2. Which endpoints are protected (need JWT token)
     * 3. How to handle requests
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (not needed for JWT-based auth)
                .csrf(AbstractHttpConfigurer::disable)

                // Use stateless sessions (no cookies, only JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Define endpoint access rules
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC endpoints - no authentication required
                        .requestMatchers("/api/auth/**").permitAll() // Các API login/register
                        .requestMatchers("/error").permitAll()       // QUAN TRỌNG: Cho phép trang báo lỗi của Spring
                        .requestMatchers("/api/users/**").authenticated()

                        // Transaction management endpoints (for future)
                        .requestMatchers("/api/transactions/**").authenticated()
                        // Cho phép vào trang chủ (nếu cần)
                        .anyRequest().authenticated()
                )

                // Add JWT filter before Spring's default authentication filter
                // This runs on every request to check JWT token
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}