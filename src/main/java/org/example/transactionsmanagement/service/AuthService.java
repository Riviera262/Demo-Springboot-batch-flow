package org.example.transactionsmanagement.service;

import lombok.RequiredArgsConstructor;
import org.example.transactionsmanagement.dto.auth.LoginRequest;
import org.example.transactionsmanagement.dto.auth.LoginResponse;
import org.example.transactionsmanagement.dto.auth.RegisterRequest;
import org.example.transactionsmanagement.dto.sysuser.SysUserResponse;
import org.example.transactionsmanagement.entity.SysUser;
import org.example.transactionsmanagement.repository.SysUserRepository;
import org.example.transactionsmanagement.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository sysUserRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SysUserService sysUserService;

    //Register user
    @Transactional
    public SysUserResponse register(RegisterRequest request) {
        //Check username and email must be unique
        boolean exists = sysUserRepository.existsByUsername(request.getUsername());
        System.out.println("DEBUG: Username " + request.getUsername() + " exists in DB? " + exists);
        if (sysUserRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }
        if (sysUserRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        //Create user
        SysUser sysUser = new SysUser();
        sysUser.setUsername(request.getUsername());
        sysUser.setPassword(passwordEncoder.encode(request.getPassword()));
        sysUser.setEmail(request.getEmail());
        sysUser.setFullName(request.getFullName());
        sysUser.setStatus("ACTIVE");
        sysUser.setRole("UPLOADER");

        //Save to DB
        sysUserRepository.save(sysUser);

        return sysUserService.convertToSysUserResponse(sysUser);
    }

    //Login user
    public LoginResponse login(LoginRequest request) {
        //Find username in DB
        SysUser sysUser = sysUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found" + request.getUsername()));

        //Check if user's status is "ACTIVE"
        if (!"ACTIVE".equals(sysUser.getStatus())) {
            throw new RuntimeException("This user account had been blocked");
        }

        //Check
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        String token = jwtUtils.generateToken(sysUser.getUsername());

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getJwtExpirationMs() / 1000) // Token expires in 1 hour
                .username(sysUser.getUsername())
                .role(sysUser.getRole())
                .build();
    }
}
