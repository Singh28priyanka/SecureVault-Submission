package com.example.securevault.service;

import com.example.securevault.dto.PasswordGenerateRequest;
import com.example.securevault.dto.PasswordGenerateResponse;
import com.example.securevault.dto.PasswordStrengthRequest;
import com.example.securevault.dto.PasswordStrengthResponse;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class PasswordIntelligenceService {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    private static final Set<String> COMMON_WORDS = Set.of(
            "password", "pass", "admin", "welcome", "login", "qwerty",
            "abc", "abcd", "letmein", "iloveyou", "monkey", "dragon",
            "master", "hello", "freedom", "whatever", "trustno1", "secure"
    );

    private static final String SEQUENTIAL_DIGITS = "0123456789";
    private static final String SEQUENTIAL_LETTERS = "abcdefghijklmnopqrstuvwxyz";

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordStrengthResponse analyzeStrength(PasswordStrengthRequest request) {
        String password = request.getPassword();
        List<String> feedback = new ArrayList<>();
        int score = 0;

        int length = password.length();
        if (length >= 16) {
            score += 2;
        } else if (length >= 12) {
            score += 1;
            feedback.add("Increase length to 16+ characters for better security");
        } else if (length >= 8) {
            feedback.add("Increase length to 12+ characters for better security");
        } else {
            feedback.add("Password is too short; use at least 8 characters");
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        if (hasUpper) {
            score += 1;
        } else {
            feedback.add("Add at least one uppercase letter");
        }

        if (hasLower) {
            score += 1;
        } else {
            feedback.add("Add at least one lowercase letter");
        }

        if (hasDigit) {
            score += 1;
        } else {
            feedback.add("Add at least one digit");
        }

        if (hasSpecial) {
            score += 1;
        } else {
            feedback.add("Add at least one special character");
        }

        if (hasRepeatedCharacters(password)) {
            score = Math.max(0, score - 1);
            feedback.add("Avoid consecutive repeated characters");
        }

        if (hasSequentialPattern(password)) {
            score = Math.max(0, score - 1);
            feedback.add("Avoid sequential patterns such as 1234 or abcd");
        }

        if (containsCommonDictionaryWord(password)) {
            score = Math.max(0, score - 2);
            feedback.add("Avoid common dictionary words");
        }

        score = Math.min(score, 5);

        if (feedback.isEmpty()) {
            feedback.add("Password looks strong");
        }

        return new PasswordStrengthResponse(score, toStrengthLabel(score), feedback);
    }

    public PasswordGenerateResponse generatePassword(PasswordGenerateRequest request) {
        StringBuilder charset = new StringBuilder();

        if (Boolean.TRUE.equals(request.getUppercase())) {
            charset.append(UPPERCASE);
        }
        if (Boolean.TRUE.equals(request.getLowercase())) {
            charset.append(LOWERCASE);
        }
        if (Boolean.TRUE.equals(request.getNumbers())) {
            charset.append(NUMBERS);
        }
        if (Boolean.TRUE.equals(request.getSymbols())) {
            charset.append(SYMBOLS);
        }

        if (charset.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one character set must be enabled"
            );
        }

        int length = request.getLength();
        StringBuilder password = new StringBuilder(length);
        String characters = charset.toString();

        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(characters.length());
            password.append(characters.charAt(index));
        }

        return new PasswordGenerateResponse(password.toString(), length);
    }

    private boolean hasRepeatedCharacters(String password) {
        for (int i = 1; i < password.length(); i++) {
            if (password.charAt(i) == password.charAt(i - 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSequentialPattern(String password) {
        String lower = password.toLowerCase();

        for (int i = 0; i <= lower.length() - 4; i++) {
            String window = lower.substring(i, i + 4);
            if (SEQUENTIAL_DIGITS.contains(window)
                    || new StringBuilder(SEQUENTIAL_DIGITS).reverse().toString().contains(window)
                    || SEQUENTIAL_LETTERS.contains(window)
                    || new StringBuilder(SEQUENTIAL_LETTERS).reverse().toString().contains(window)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsCommonDictionaryWord(String password) {
        String lower = password.toLowerCase();
        return COMMON_WORDS.stream().anyMatch(lower::contains);
    }

    private String toStrengthLabel(int score) {
        return switch (score) {
            case 0, 1 -> "Very Weak";
            case 2 -> "Weak";
            case 3 -> "Fair";
            case 4 -> "Strong";
            default -> "Very Strong";
        };
    }
}
