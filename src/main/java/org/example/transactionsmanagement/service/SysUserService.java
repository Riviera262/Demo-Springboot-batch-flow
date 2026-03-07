package org.example.transactionsmanagement.service;

import lombok.RequiredArgsConstructor;
import org.example.transactionsmanagement.dto.sysuser.SysUserResponse;
import org.example.transactionsmanagement.entity.SysUser;
import org.example.transactionsmanagement.repository.SysUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserRepository sysUserRepository;

    public SysUserResponse getCurrentUserProfile(){
        String username = getCurrentUsername();

        SysUser sysUser = sysUserRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found "+ username));
        return convertToSysUserResponse(sysUser);
    }

    public SysUserResponse getUserByUsername(String username){
        SysUser sysUser = sysUserRepository.findByUsername(username)
                .orElseThrow(()-> new RuntimeException("User not found "+ username));
        return convertToSysUserResponse(sysUser);
    }

    public SysUserResponse convertToSysUserResponse(SysUser sysUser){
        return SysUserResponse.builder()
                .username(sysUser.getUsername())
                .email(sysUser.getEmail())
                .fullName(sysUser.getFullName())
                .role(sysUser.getRole())
                .status(sysUser.getStatus())
                .build();
    }

    public String getCurrentUsername(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
