package com.example.expense_split.controller;

import com.example.expense_split.dto.AuthenticationResponse;
import com.example.expense_split.dto.LoginRequest;
import com.example.expense_split.dto.RegisterRequest;
import com.example.expense_split.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.expense_split.dto.ResponseTypeDto;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/register")
    public ResponseEntity<ResponseTypeDto<AuthenticationResponse>> register(
            @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(ResponseTypeDto.success("Registration successful", service.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseTypeDto<AuthenticationResponse>> authenticate(
            @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(ResponseTypeDto.success("Login successful", service.authenticate(request)));
    }
}
