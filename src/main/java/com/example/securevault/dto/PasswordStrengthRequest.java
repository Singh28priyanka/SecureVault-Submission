package com.example.securevault.dto;

import jakarta.validation.constraints.NotBlank;

public class PasswordStrengthRequest {

    @NotBlank(message = "Password is required")
    private String password;

    public PasswordStrengthRequest() {
    }

    public PasswordStrengthRequest(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
