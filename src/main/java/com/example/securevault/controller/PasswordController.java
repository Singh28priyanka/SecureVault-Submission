package com.example.securevault.controller;

import com.example.securevault.dto.PasswordGenerateRequest;
import com.example.securevault.dto.PasswordGenerateResponse;
import com.example.securevault.dto.PasswordStrengthRequest;
import com.example.securevault.dto.PasswordStrengthResponse;
import com.example.securevault.dto.response.ApiResponse;
import com.example.securevault.service.PasswordIntelligenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/password")
public class PasswordController {

    private final PasswordIntelligenceService passwordIntelligenceService;

    public PasswordController(PasswordIntelligenceService passwordIntelligenceService) {
        this.passwordIntelligenceService = passwordIntelligenceService;
    }

    @PostMapping("/strength")
    public ResponseEntity<ApiResponse<PasswordStrengthResponse>> analyzeStrength(
            @Valid @RequestBody PasswordStrengthRequest request) {

        PasswordStrengthResponse result =
                passwordIntelligenceService.analyzeStrength(request);

        return ResponseEntity.ok(
                ApiResponse.success("Password strength analyzed successfully", result)
        );
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<PasswordGenerateResponse>> generatePassword(
            @Valid @RequestBody PasswordGenerateRequest request) {

        PasswordGenerateResponse result =
                passwordIntelligenceService.generatePassword(request);

        return ResponseEntity.ok(
                ApiResponse.success("Password generated successfully", result)
        );
    }
}
