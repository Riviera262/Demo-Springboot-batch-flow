package org.example.transactionsmanagement.security;

import org.example.transactionsmanagement.entity.SysUser;
import org.example.transactionsmanagement.repository.SysUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsServiceImpl - Load User from Database
 *
 * Spring Security calls this when:
 * 1. User logs in (to verify password)
 * 2. JWT filter verifies token (to get user's roles)
 *
 * This is the bridge between Spring Security and your database.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserRepository sysUserRepository;

    /**
     * Load user by username
     *
     * Called by Spring Security to:
     * 1. Find user in database
     * 2. Wrap in CustomUserDetails
     * 3. Return to Spring Security
     *
     * @param username - username to find
     * @return UserDetails object (CustomUserDetails wrapping SysUser)
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Find user in database
        SysUser sysUser = sysUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // 2. Wrap in CustomUserDetails and return
        return new CustomUserDetails(sysUser);
    }
}