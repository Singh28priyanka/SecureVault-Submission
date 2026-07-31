package com.example.securevault.mapper;

import com.example.securevault.dto.CredentialRequest;
import com.example.securevault.dto.CredentialResponse;
import com.example.securevault.dto.CredentialUpdateRequest;
import com.example.securevault.entity.Category;
import com.example.securevault.entity.Credential;
import com.example.securevault.entity.User;
import org.springframework.stereotype.Component;

@Component
public class CredentialMapper {

    public Credential toEntity(CredentialRequest request,
                               User owner,
                               Category category,
                               String encryptedPassword) {
        Credential credential = new Credential();
        credential.setTitle(request.getTitle());
        credential.setUsername(request.getUsername());
        credential.setEncryptedPassword(encryptedPassword);
        credential.setWebsiteUrl(request.getWebsiteUrl());
        credential.setNotes(request.getNotes());
        credential.setUser(owner);
        credential.setCategory(category);
        credential.setDeleted(false);
        credential.setDeletedAt(null);
        return credential;
    }

    public void applyUpdate(Credential credential, CredentialUpdateRequest request) {
        if (request.getTitle() != null) {
            credential.setTitle(request.getTitle());
        }
        if (request.getUsername() != null) {
            credential.setUsername(request.getUsername());
        }
        if (request.getWebsiteUrl() != null) {
            credential.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getNotes() != null) {
            credential.setNotes(request.getNotes());
        }
    }

    public CredentialResponse toResponse(Credential credential, String decryptedPassword) {
        Category category = credential.getCategory();
        return new CredentialResponse(
                credential.getId(),
                credential.getTitle(),
                credential.getUsername(),
                decryptedPassword,
                credential.getWebsiteUrl(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                credential.getNotes(),
                credential.getUser().getId(),
                credential.isDeleted(),
                credential.getDeletedAt()
        );
    }
}
