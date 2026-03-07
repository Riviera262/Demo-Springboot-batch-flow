package org.example.transactionsmanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter - JWT Verification Filter
 *
 * This filter runs BEFORE every request reaches your controller.
 *
 * What it does:
 * 1. Extract JWT token from request header
 * 2. Validate the token
 * 3. Get username from token
 * 4. Load user from database
 * 5. Put user in SecurityContext
 * 6. Let request continue to controller
 *
 * If token is invalid/missing, request continues but user is not authenticated.
 * SecurityConfig will then block protected endpoints.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    /**
     * This method runs on EVERY request
     *
     * Flow:
     * Request comes in → This filter runs → Controller executes
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Step 1: Extract JWT token from header
            String jwt = parseJwt(request);

            // Step 2: If JWT exists and is valid
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                // Step 3: Get username from token
                String username = jwtUtils.getUsernameFromJwtToken(jwt);

                // Step 4: Load user from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Step 5: Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()  // User's roles
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Step 6: Put user in SecurityContext
                // Now Spring Security knows who the user is!
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            System.err.println("Cannot set user authentication: " + e);
        }

        // Step 7: Continue to next filter or controller
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from request header
     *
     * Header format: "Authorization: Bearer <token>"
     * We extract just the <token> part
     *
     * @param request - HTTP request
     * @return JWT token string, or null if not found
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // Remove "Bearer " prefix
        }

        return null;
    }
}