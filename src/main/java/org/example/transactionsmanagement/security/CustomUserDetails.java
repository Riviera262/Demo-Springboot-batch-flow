package org.example.transactionsmanagement.security;

import org.example.transactionsmanagement.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * CustomUserDetails - Wrapper for SysUser
 *
 * Spring Security needs UserDetails interface.
 * This class wraps our SysUser entity to work with Spring Security.
 *
 * Think of it as an "adapter" - converts our user format to Spring's format.
 */
public class CustomUserDetails implements UserDetails {

    private final SysUser sysUser;

    public CustomUserDetails(SysUser sysUser) {
        this.sysUser = sysUser;
    }

    /**
     * Get user's roles/permissions
     * Spring Security uses this to check if user can access endpoints
     *
     * Example: If user has role "ADMIN", this returns ["ROLE_ADMIN"]
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convert role to Spring Security format
        // "ADMIN" becomes "ROLE_ADMIN"
        return Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + sysUser.getRole())
        );
    }

    /**
     * Get user's password (hashed)
     * Spring Security uses this to verify password during login
     */
    @Override
    public String getPassword() {
        return sysUser.getPassword();  // ✅ FIXED: returns actual password from entity
    }

    /**
     * Get username
     */
    @Override
    public String getUsername() {
        return sysUser.getUsername();  // ✅ FIXED: returns actual username from entity
    }

    /**
     * Is account not expired?
     * We don't use account expiration, so always return true
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Is account not locked?
     * We check this using the STATUS field
     */
    @Override
    public boolean isAccountNonLocked() {
        return !"LOCKED".equals(sysUser.getStatus());
    }

    /**
     * Are credentials (password) not expired?
     * We don't use password expiration, so always return true
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Is account enabled?
     * Account is enabled if status is ACTIVE
     */
    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(sysUser.getStatus());
    }

    /**
     * Get the wrapped SysUser entity
     * Useful if you need access to other fields like email
     */
    public SysUser getSysUser() {
        return sysUser;
    }
}