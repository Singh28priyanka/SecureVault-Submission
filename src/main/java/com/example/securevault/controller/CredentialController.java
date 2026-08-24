package com.example.securevault.controller;

import com.example.securevault.dto.CredentialRequest;
import com.example.securevault.dto.CredentialResponse;
import com.example.securevault.dto.CredentialUpdateRequest;
import com.example.securevault.dto.VaultQueryRequest;
import com.example.securevault.dto.response.ApiResponse;
import com.example.securevault.dto.response.PageResponse;
import com.example.securevault.security.AuthUser;
import com.example.securevault.service.CredentialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vault")
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CredentialResponse>> createCredential(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CredentialRequest request) {

        CredentialResponse created =
                credentialService.createCredential(authUser.id(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Credential created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CredentialResponse>> getCredential(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id) {

        CredentialResponse credential =
                credentialService.getCredentialById(id, authUser.id());

        return ResponseEntity.ok(
                ApiResponse.success("Credential retrieved successfully", credential)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CredentialResponse>>> getCredentials(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String website) {

        VaultQueryRequest query = buildQuery(page, size, sortBy, direction, category, title, username, website);

        PageResponse<CredentialResponse> result =
                credentialService.getCredentials(authUser.id(), query);

        return ResponseEntity.ok(
                ApiResponse.success("Credentials retrieved successfully", result)
        );
    }

    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<PageResponse<CredentialResponse>>> getTrash(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        VaultQueryRequest query = buildQuery(page, size, sortBy, direction, null, null, null, null);

        PageResponse<CredentialResponse> result =
                credentialService.getTrashCredentials(authUser.id(), query);

        return ResponseEntity.ok(
                ApiResponse.success("Trash retrieved successfully", result)
        );
    }

    @PutMapping("/{credentialId}")
    public ResponseEntity<ApiResponse<CredentialResponse>> updateCredential(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long credentialId,
            @Valid @RequestBody CredentialUpdateRequest request) {

        CredentialResponse updated =
                credentialService.updateCredential(
                        credentialId,
                        authUser.id(),
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success("Credential updated successfully", updated)
        );
    }

    @PutMapping("/{credentialId}/restore")
    public ResponseEntity<ApiResponse<CredentialResponse>> restoreCredential(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long credentialId) {

        CredentialResponse restored =
                credentialService.restoreCredential(credentialId, authUser.id());

        return ResponseEntity.ok(
                ApiResponse.success("Credential restored successfully", restored)
        );
    }

    @DeleteMapping("/{credentialId}")
    public ResponseEntity<ApiResponse<Void>> deleteCredential(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long credentialId) {

        credentialService.deleteCredential(credentialId, authUser.id());

        return ResponseEntity.ok(
                ApiResponse.success("Credential deleted successfully")
        );
    }

    @DeleteMapping("/{credentialId}/permanent")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteCredential(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long credentialId) {

        credentialService.permanentlyDeleteCredential(credentialId, authUser.id());

        return ResponseEntity.ok(
                ApiResponse.success("Credential permanently deleted")
        );
    }

    private VaultQueryRequest buildQuery(int page,
                                         int size,
                                         String sortBy,
                                         String direction,
                                         String category,
                                         String title,
                                         String username,
                                         String website) {
        VaultQueryRequest query = new VaultQueryRequest();
        query.setPage(page);
        query.setSize(size);
        query.setSortBy(sortBy);
        query.setDirection(direction);
        query.setCategory(category);
        query.setTitle(title);
        query.setUsername(username);
        query.setWebsite(website);
        return query;
    }
}
