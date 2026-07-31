package com.example.securevault.controller;

import com.example.securevault.dto.AuthResponse;
import com.example.securevault.dto.LoginRequest;
import com.example.securevault.dto.RegisterRequest;
import com.example.securevault.dto.UserResponse;
import com.example.securevault.dto.response.ApiResponse;
import com.example.securevault.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.securevault.security.AuthUser;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse user = userService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse auth = userService.loginUser(request);

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", auth)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal AuthUser authUser) {

        UserResponse profile = userService.getUserProfile(authUser.id());

        return ResponseEntity.ok(
                ApiResponse.success("Profile retrieved successfully", profile)
        );
    }
}
