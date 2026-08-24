package com.example.securevault.service;

import com.example.securevault.config.CacheNames;
import com.example.securevault.dto.CategoryResponse;
import com.example.securevault.dto.CategoryUpdateRequest;
import com.example.securevault.entity.Category;
import com.example.securevault.entity.User;
import com.example.securevault.exception.UnauthorizedAccessException;
import com.example.securevault.exception.UserNotFoundException;
import com.example.securevault.repository.CategoryRepository;
import com.example.securevault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public CategoryService(CategoryRepository categoryRepository,
                           UserRepository userRepository,
                           CacheManager cacheManager) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Cached category list for a user. Body runs only on cache miss.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.CATEGORIES, key = "#userId")
    public List<CategoryResponse> getCategoriesForUser(Long userId) {
        log.info("CACHE MISS categories key={} — loading from database", userId);
        return categoryRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Resolves a category by id or name for the given user.
     * When a new category is created, the categories cache for that user is evicted.
     */
    @Transactional
    public Category resolveForUser(Long userId, Long categoryId, String categoryName) {
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            if (!category.getUser().getId().equals(userId)) {
                throw new UnauthorizedAccessException(
                        "You are not allowed to access this category"
                );
            }
            return category;
        }

        if (StringUtils.hasText(categoryName)) {
            String normalized = categoryName.trim();
            return categoryRepository.findByUserIdAndNameIgnoreCase(userId, normalized)
                    .orElseGet(() -> persistNewCategory(userId, normalized, null));
        }

        return null;
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.CATEGORIES, key = "#userId")
    public CategoryResponse createCategory(Long userId, String name, String description) {
        Category saved = persistNewCategory(userId, name, description);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.CATEGORIES, key = "#userId")
    public CategoryResponse updateCategory(Long userId,
                                           Long categoryId,
                                           CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException(
                    "You are not allowed to access this category"
            );
        }

        if (StringUtils.hasText(request.getName())) {
            category.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        Category saved = categoryRepository.save(category);
        log.info("Category updated id={} userId={} — categories cache evicted",
                saved.getId(), userId);
        return toResponse(saved);
    }

    private Category persistNewCategory(Long userId, String name, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        Category saved = categoryRepository.save(new Category(name, description, user));
        evictCategoriesCache(userId);
        log.info("Category created id={} userId={} — categories cache evicted",
                saved.getId(), userId);
        return saved;
    }

    private void evictCategoriesCache(Long userId) {
        Cache cache = cacheManager.getCache(CacheNames.CATEGORIES);
        if (cache != null) {
            cache.evict(userId);
        }
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getUser().getId()
        );
    }
}
