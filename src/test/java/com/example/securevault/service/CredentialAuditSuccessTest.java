package com.example.securevault.service;

import com.example.securevault.dto.CredentialRequest;
import com.example.securevault.dto.CredentialUpdateRequest;
import com.example.securevault.entity.AuditLog;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a successful credential update persists both the credential
 * change and a matching audit log entry in the same committed transaction.
 */
@SpringBootTest
class CredentialAuditSuccessTest {

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

    private User user;
    private Long credentialId;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        credentialRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User(
                "Audit User",
                "audit-success@securevault.test",
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
    void successfulUpdateCreatesUpdatedCredentialAndAuditLog() {
        CredentialUpdateRequest updateRequest = new CredentialUpdateRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setUsername("updated@example.com");

        credentialService.updateCredential(credentialId, user.getId(), updateRequest);

        Credential credential = credentialRepository.findById(credentialId).orElseThrow();
        assertThat(credential.getTitle()).isEqualTo("Updated Title");
        assertThat(credential.getUsername()).isEqualTo("updated@example.com");

        List<AuditLog> updateLogs = auditLogRepository
                .findByEntityTypeAndEntityIdOrderByTimestampDesc(
                        AuditActions.ENTITY_CREDENTIAL,
                        credentialId
                )
                .stream()
                .filter(log -> AuditActions.UPDATED.equals(log.getAction()))
                .toList();

        assertThat(updateLogs).hasSize(1);
        AuditLog auditLog = updateLogs.get(0);
        assertThat(auditLog.getEntityType()).isEqualTo(AuditActions.ENTITY_CREDENTIAL);
        assertThat(auditLog.getEntityId()).isEqualTo(credentialId);
        assertThat(auditLog.getPerformedById()).isEqualTo(user.getId());
        assertThat(auditLog.getTimestamp()).isNotNull();

        assertThat(auditLogRepository.countByEntityTypeAndEntityIdAndAction(
                AuditActions.ENTITY_CREDENTIAL,
                credentialId,
                AuditActions.CREATED
        )).isEqualTo(1);
    }
}
