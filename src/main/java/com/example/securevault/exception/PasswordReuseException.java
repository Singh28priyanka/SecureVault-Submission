package com.example.securevault.exception;

public class PasswordReuseException extends RuntimeException {

    public PasswordReuseException(String message) {
        super(message);
    }
}
