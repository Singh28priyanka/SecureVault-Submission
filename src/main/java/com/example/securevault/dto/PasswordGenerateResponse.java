package com.example.securevault.dto;

public class PasswordGenerateResponse {

    private String password;
    private int length;

    public PasswordGenerateResponse() {
    }

    public PasswordGenerateResponse(String password, int length) {
        this.password = password;
        this.length = length;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
