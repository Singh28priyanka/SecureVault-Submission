package com.example.securevault.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PasswordGenerateRequest {

    @NotNull(message = "Length is required")
    @Min(value = 8, message = "Password length must be at least 8")
    @Max(value = 128, message = "Password length must not exceed 128")
    private Integer length;

    @NotNull(message = "uppercase flag is required")
    private Boolean uppercase;

    @NotNull(message = "lowercase flag is required")
    private Boolean lowercase;

    @NotNull(message = "numbers flag is required")
    private Boolean numbers;

    @NotNull(message = "symbols flag is required")
    private Boolean symbols;

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Boolean getUppercase() {
        return uppercase;
    }

    public void setUppercase(Boolean uppercase) {
        this.uppercase = uppercase;
    }

    public Boolean getLowercase() {
        return lowercase;
    }

    public void setLowercase(Boolean lowercase) {
        this.lowercase = lowercase;
    }

    public Boolean getNumbers() {
        return numbers;
    }

    public void setNumbers(Boolean numbers) {
        this.numbers = numbers;
    }

    public Boolean getSymbols() {
        return symbols;
    }

    public void setSymbols(Boolean symbols) {
        this.symbols = symbols;
    }
}
