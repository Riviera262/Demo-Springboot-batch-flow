package org.example.transactionsmanagement.controller;

import lombok.RequiredArgsConstructor;
import org.example.transactionsmanagement.dto.sysuser.SysUserResponse;
import org.example.transactionsmanagement.service.SysUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class SysUserController {
    private final SysUserService sysUserService;

    @GetMapping("/me")
    public ResponseEntity<SysUserResponse> getCurrentSysUser(){
        SysUserResponse sysUserResponse = sysUserService.getCurrentUserProfile();
        return ResponseEntity.status(HttpStatus.OK).body(sysUserResponse);
    }

}
