package com.example.securevault.dto;

import java.time.LocalDateTime;

public class CredentialResponse {

    private Long id;
    private String title;
    private String username;
    private String password;
    private String websiteUrl;
    private Long categoryId;
    private String category;
    private String notes;
    private Long userId;
    private boolean deleted;
    private LocalDateTime deletedAt;

    public CredentialResponse() {
    }

    public CredentialResponse(Long id,
                              String title,
                              String username,
                              String password,
                              String websiteUrl,
                              Long categoryId,
                              String category,
                              String notes,
                              Long userId,
                              boolean deleted,
                              LocalDateTime deletedAt) {
        this.id = id;
        this.title = title;
        this.username = username;
        this.password = password;
        this.websiteUrl = websiteUrl;
        this.categoryId = categoryId;
        this.category = category;
        this.notes = notes;
        this.userId = userId;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
