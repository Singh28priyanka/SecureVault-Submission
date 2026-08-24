package com.example.securevault.service;

import com.example.securevault.config.CacheNames;
import com.example.securevault.dto.CredentialRequest;
import com.example.securevault.dto.CredentialResponse;
import com.example.securevault.dto.CredentialUpdateRequest;
import com.example.securevault.dto.VaultQueryRequest;
import com.example.securevault.dto.response.PageResponse;
import com.example.securevault.entity.Category;
import com.example.securevault.entity.Credential;
import com.example.securevault.entity.PasswordHistory;
import com.example.securevault.entity.User;
import com.example.securevault.exception.CredentialNotFoundException;
import com.example.securevault.exception.PasswordReuseException;
import com.example.securevault.exception.UnauthorizedAccessException;
import com.example.securevault.exception.UserNotFoundException;
import com.example.securevault.mapper.CredentialMapper;
import com.example.securevault.repository.CredentialRepository;
import com.example.securevault.repository.CredentialSpecifications;
import com.example.securevault.repository.PasswordHistoryRepository;
import com.example.securevault.repository.UserRepository;
import com.example.securevault.util.AESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class CredentialService {

    private static final Logger log = LoggerFactory.getLogger(CredentialService.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "title", "username", "category", "websiteUrl", "createdAt", "updatedAt", "id"
    );

    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final AESUtil aesUtil;
    private final CredentialMapper credentialMapper;
    private final AuditLogService auditLogService;
    private final CategoryService categoryService;

    public CredentialService(CredentialRepository credentialRepository,
                             UserRepository userRepository,
                             PasswordHistoryRepository passwordHistoryRepository,
                             AESUtil aesUtil,
                             CredentialMapper credentialMapper,
                             AuditLogService auditLogService,
                             CategoryService categoryService) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.aesUtil = aesUtil;
        this.credentialMapper = credentialMapper;
        this.auditLogService = auditLogService;
        this.categoryService = categoryService;
    }

    @Transactional
    public CredentialResponse createCredential(Long userId, CredentialRequest request) {
        User user = findUser(userId);
        Category category = categoryService.resolveForUser(
                userId, request.getCategoryId(), request.getCategory());
        String encryptedPassword = aesUtil.encrypt(request.getPassword());

        Credential credential = credentialMapper.toEntity(request, user, category, encryptedPassword);
        Credential saved = credentialRepository.save(credential);

        auditLogService.record(
                AuditActions.CREATED,
                AuditActions.ENTITY_CREDENTIAL,
                saved.getId(),
                userId
        );

        log.info("Credential created id={} userId={}", saved.getId(), userId);
        return credentialMapper.toResponse(saved, request.getPassword());
    }

    /**
     * Cached credential details. Method body runs only on cache miss.
     * Key includes userId so one owner's cache entry cannot be served to another user.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.CREDENTIALS, key = "#credentialId + '-' + #userId")
    public CredentialResponse getCredentialById(Long credentialId, Long userId) {
        log.info("CACHE MISS credentials key={}-{} — loading from database",
                credentialId, userId);
        Credential credential = findOwnedActiveCredential(credentialId, userId);
        return toResponse(credential);
    }

    @Transactional(readOnly = true)
    public PageResponse<CredentialResponse> getCredentials(Long userId, VaultQueryRequest query) {
        Pageable pageable = buildPageable(query);

        Specification<Credential> spec = CredentialSpecifications.withFilters(
                userId,
                query.getCategory(),
                query.getTitle(),
                query.getUsername(),
                query.getWebsite()
        );

        Page<CredentialResponse> page = credentialRepository.findAll(spec, pageable)
                .map(this::toResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<CredentialResponse> getTrashCredentials(Long userId, VaultQueryRequest query) {
        Pageable pageable = buildPageable(query);

        Specification<Credential> spec = CredentialSpecifications.trashForUser(userId);

        Page<CredentialResponse> page = credentialRepository.findAll(spec, pageable)
                .map(this::toResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public List<CredentialResponse> getAllCredentialsUnoptimized(Long userId) {
        return credentialRepository.findByUserIdAndDeletedFalse(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CredentialResponse> getAllCredentialsOptimized(Long userId) {
        return credentialRepository.findByUserIdWithUser(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CachePut(cacheNames = CacheNames.CREDENTIALS, key = "#credentialId + '-' + #userId")
    public CredentialResponse updateCredential(Long credentialId,
                                               Long userId,
                                               CredentialUpdateRequest request) {
        Credential credential = findOwnedActiveCredential(credentialId, userId);
        credentialMapper.applyUpdate(credential, request);

        if (request.getCategoryId() != null || StringUtils.hasText(request.getCategory())) {
            Category category = categoryService.resolveForUser(
                    userId, request.getCategoryId(), request.getCategory());
            credential.setCategory(category);
        }

        String plainPasswordForResponse = null;
        if (StringUtils.hasText(request.getPassword())) {
            applyPasswordChange(credential, request.getPassword());
            plainPasswordForResponse = request.getPassword();
        }

        Credential updated = credentialRepository.save(credential);

        auditLogService.record(
                AuditActions.UPDATED,
                AuditActions.ENTITY_CREDENTIAL,
                updated.getId(),
                userId
        );

        log.info("Credential updated id={} userId={} — credentials cache refreshed (@CachePut)",
                updated.getId(), userId);
        return toResponse(updated, plainPasswordForResponse);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.CREDENTIALS, key = "#credentialId + '-' + #userId")
    public void deleteCredential(Long credentialId, Long userId) {
        Credential credential = findOwnedActiveCredential(credentialId, userId);
        Long entityId = credential.getId();

        credential.setDeleted(true);
        credential.setDeletedAt(LocalDateTime.now());
        credentialRepository.save(credential);

        auditLogService.record(
                AuditActions.DELETED,
                AuditActions.ENTITY_CREDENTIAL,
                entityId,
                userId
        );

        log.info("Credential soft-deleted id={} userId={} — credentials cache evicted",
                entityId, userId);
    }

    @Transactional
    @CachePut(cacheNames = CacheNames.CREDENTIALS, key = "#credentialId + '-' + #userId")
    public CredentialResponse restoreCredential(Long credentialId, Long userId) {
        Credential credential = findOwnedDeletedCredential(credentialId, userId);

        credential.setDeleted(false);
        credential.setDeletedAt(null);
        Credential restored = credentialRepository.save(credential);

        auditLogService.record(
                AuditActions.RESTORED,
                AuditActions.ENTITY_CREDENTIAL,
                restored.getId(),
                userId
        );

        log.info("Credential restored id={} userId={} — credentials cache refreshed",
                restored.getId(), userId);
        return toResponse(restored);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CREDENTIALS, key = "#credentialId + '-' + #userId")
    })
    public void permanentlyDeleteCredential(Long credentialId, Long userId) {
        Credential credential = findOwnedDeletedCredential(credentialId, userId);
        Long entityId = credential.getId();

        passwordHistoryRepository.deleteByCredential_Id(entityId);
        credentialRepository.delete(credential);

        auditLogService.record(
                AuditActions.PERMANENTLY_DELETED,
                AuditActions.ENTITY_CREDENTIAL,
                entityId,
                userId
        );

        log.info("Credential permanently deleted id={} userId={} — credentials cache evicted",
                entityId, userId);
    }

    private void applyPasswordChange(Credential credential, String newPlainPassword) {
        rejectPasswordReuse(credential, newPlainPassword);

        int nextVersion = passwordHistoryRepository
                .findTopByCredential_IdOrderByVersionDesc(credential.getId())
                .map(history -> history.getVersion() + 1)
                .orElse(1);

        PasswordHistory archive = new PasswordHistory();
        archive.setCredential(credential);
        archive.setEncryptedPassword(credential.getEncryptedPassword());
        archive.setVersion(nextVersion);
        passwordHistoryRepository.save(archive);

        credential.setEncryptedPassword(aesUtil.encrypt(newPlainPassword));
    }

    private void rejectPasswordReuse(Credential credential, String newPlainPassword) {
        List<PasswordHistory> recentHistory = passwordHistoryRepository
                .findTop5ByCredential_IdOrderByVersionDesc(credential.getId());

        for (PasswordHistory history : recentHistory) {
            if (Objects.equals(aesUtil.decrypt(history.getEncryptedPassword()), newPlainPassword)) {
                throw new PasswordReuseException(
                        "Password was used recently and cannot be reused"
                );
            }
        }
    }

    private Pageable buildPageable(VaultQueryRequest query) {
        int page = Math.max(query.getPage(), 0);
        int size = query.getSize() < 1 ? 10 : Math.min(query.getSize(), 100);

        String sortBy = ALLOWED_SORT_FIELDS.contains(query.getSortBy())
                ? query.getSortBy()
                : "title";

        String sortProperty = "category".equals(sortBy) ? "category.name" : sortBy;

        Sort.Direction direction =
                "desc".equalsIgnoreCase(query.getDirection())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, sortProperty));
    }

    private CredentialResponse toResponse(Credential credential) {
        return toResponse(credential, null);
    }

    private CredentialResponse toResponse(Credential credential, String plainPasswordOverride) {
        String decryptedPassword = plainPasswordOverride != null
                ? plainPasswordOverride
                : aesUtil.decrypt(credential.getEncryptedPassword());
        return credentialMapper.toResponse(credential, decryptedPassword);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private Credential findOwnedActiveCredential(Long credentialId, Long userId) {
        Credential credential = credentialRepository.findActiveWithUserById(credentialId)
                .orElseThrow(() -> new CredentialNotFoundException("Credential not found"));
        assertOwnership(credential, userId);
        return credential;
    }

    private Credential findOwnedDeletedCredential(Long credentialId, Long userId) {
        Credential credential = credentialRepository.findDeletedWithUserById(credentialId)
                .orElseThrow(() -> new CredentialNotFoundException("Credential not found in trash"));
        assertOwnership(credential, userId);
        return credential;
    }

    private void assertOwnership(Credential credential, Long userId) {
        if (!credential.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException(
                    "You are not allowed to access this credential"
            );
        }
    }
}
