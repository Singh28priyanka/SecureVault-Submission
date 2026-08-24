package com.example.securevault.dto;

import java.util.List;

public class PasswordStrengthResponse {

    private int score;
    private String strength;
    private List<String> feedback;

    public PasswordStrengthResponse() {
    }

    public PasswordStrengthResponse(int score, String strength, List<String> feedback) {
        this.score = score;
        this.strength = strength;
        this.feedback = feedback;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public List<String> getFeedback() {
        return feedback;
    }

    public void setFeedback(List<String> feedback) {
        this.feedback = feedback;
    }
}
