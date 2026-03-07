package org.example.transactionsmanagement.controller;

import lombok.RequiredArgsConstructor;
import org.example.transactionsmanagement.dto.auth.LoginRequest;
import org.example.transactionsmanagement.dto.auth.LoginResponse;
import org.example.transactionsmanagement.dto.auth.RegisterRequest;
import org.example.transactionsmanagement.dto.sysuser.SysUserResponse;
import org.example.transactionsmanagement.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<SysUserResponse> register(@RequestBody RegisterRequest request){
        SysUserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request){
        LoginResponse response = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


}
