package com.example.securevault.controller;

import com.example.securevault.dto.CategoryResponse;
import com.example.securevault.dto.CategoryUpdateRequest;
import com.example.securevault.dto.response.ApiResponse;
import com.example.securevault.security.AuthUser;
import com.example.securevault.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories(
            @AuthenticationPrincipal AuthUser authUser) {

        List<CategoryResponse> categories =
                categoryService.getCategoriesForUser(authUser.id());

        return ResponseEntity.ok(
                ApiResponse.success("Categories retrieved successfully", categories)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CategoryUpdateRequest request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }

        CategoryResponse created = categoryService.createCategory(
                authUser.id(), request.getName().trim(), request.getDescription());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {

        CategoryResponse updated =
                categoryService.updateCategory(authUser.id(), id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Category updated successfully", updated)
        );
    }
}
