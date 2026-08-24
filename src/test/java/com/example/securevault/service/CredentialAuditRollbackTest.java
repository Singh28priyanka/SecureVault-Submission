package com.example.securevault.service;

import com.example.securevault.dto.CredentialRequest;
import com.example.securevault.dto.CredentialUpdateRequest;
import com.example.securevault.entity.Credential;
import com.example.securevault.entity.User;
import com.example.securevault.repository.AuditLogRepository;
import com.example.securevault.repository.CategoryRepository;
import com.example.securevault.repository.CredentialRepository;
import com.example.securevault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

/**
 * Verifies that when audit logging fails during an update, the credential
 * change is rolled back with the surrounding transaction.
 */
@SpringBootTest
class CredentialAuditRollbackTest {

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @MockitoSpyBean
    private AuditLogService auditLogService;

    private User user;
    private Long credentialId;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        credentialRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User(
                "Rollback User",
                "audit-rollback@securevault.test",
                passwordEncoder.encode("Password1")
        ));

        CredentialRequest createRequest = new CredentialRequest();
        createRequest.setTitle("Original Title");
        createRequest.setUsername("original@example.com");
        createRequest.setPassword("SecretPass1");
        createRequest.setCategory("WORK");
        createRequest.setWebsiteUrl("https://example.com");

        credentialId = credentialService.createCredential(user.getId(), createRequest).getId();
    }

    @Test
    void updateRollsBackWhenAuditRecordFails() {
        doThrow(new RuntimeException("Simulated audit failure"))
                .when(auditLogService)
                .record(
                        eq(AuditActions.UPDATED),
                        eq(AuditActions.ENTITY_CREDENTIAL),
                        eq(credentialId),
                        eq(user.getId())
                );

        CredentialUpdateRequest updateRequest = new CredentialUpdateRequest();
        updateRequest.setTitle("Should Not Persist");
        updateRequest.setUsername("should-not-persist@example.com");

        assertThatThrownBy(() ->
                credentialService.updateCredential(credentialId, user.getId(), updateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated audit failure");

        Credential credential = credentialRepository.findById(credentialId).orElseThrow();
        assertThat(credential.getTitle()).isEqualTo("Original Title");
        assertThat(credential.getUsername()).isEqualTo("original@example.com");

        assertThat(auditLogRepository.countByEntityTypeAndEntityIdAndAction(
                AuditActions.ENTITY_CREDENTIAL,
                credentialId,
                AuditActions.UPDATED
        )).isZero();
    }
}
