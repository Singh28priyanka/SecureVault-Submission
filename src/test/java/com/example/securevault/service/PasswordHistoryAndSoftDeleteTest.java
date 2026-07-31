package com.example.securevault.service;

import com.example.securevault.dto.CredentialRequest;
import com.example.securevault.dto.CredentialResponse;
import com.example.securevault.dto.CredentialUpdateRequest;
import com.example.securevault.dto.VaultQueryRequest;
import com.example.securevault.entity.PasswordHistory;
import com.example.securevault.entity.User;
import com.example.securevault.exception.CredentialNotFoundException;
import com.example.securevault.exception.PasswordReuseException;
import com.example.securevault.repository.AuditLogRepository;
import com.example.securevault.repository.CategoryRepository;
import com.example.securevault.repository.CredentialRepository;
import com.example.securevault.repository.PasswordHistoryRepository;
import com.example.securevault.repository.UserRepository;
import com.example.securevault.util.AESUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PasswordHistoryAndSoftDeleteTest {

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User user;
    private Long credentialId;

    @BeforeEach
    void setUp() {
        passwordHistoryRepository.deleteAll();
        auditLogRepository.deleteAll();
        credentialRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User(
                "History User",
                "history-soft-delete@securevault.test",
                passwordEncoder.encode("Password1")
        ));

        CredentialRequest createRequest = new CredentialRequest();
        createRequest.setTitle("Vault Entry");
        createRequest.setUsername("user@example.com");
        createRequest.setPassword("SecretPass1");
        createRequest.setCategory("WORK");
        createRequest.setWebsiteUrl("https://example.com");

        credentialId = credentialService.createCredential(user.getId(), createRequest).getId();
    }

    @Test
    void passwordUpdateArchivesOldPasswordIntoHistory() {
        CredentialUpdateRequest updateRequest = new CredentialUpdateRequest();
        updateRequest.setPassword("NewPass2!");

        credentialService.updateCredential(credentialId, user.getId(), updateRequest);

        List<PasswordHistory> history =
                passwordHistoryRepository.findTop5ByCredential_IdOrderByVersionDesc(credentialId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getVersion()).isEqualTo(1);
        assertThat(aesUtil.decrypt(history.get(0).getEncryptedPassword()))
                .isEqualTo("SecretPass1");
    }

    @Test
    void reusingOneOfLastFivePasswordsThrowsPasswordReuseException() {
        String[] passwords = {
                "Pass2!", "Pass3!", "Pass4!", "Pass5!", "Pass6!", "Pass7!"
        };

        for (String password : passwords) {
            CredentialUpdateRequest updateRequest = new CredentialUpdateRequest();
            updateRequest.setPassword(password);
            credentialService.updateCredential(credentialId, user.getId(), updateRequest);
        }

        CredentialUpdateRequest reuseRequest = new CredentialUpdateRequest();
        reuseRequest.setPassword("Pass4!");

        assertThatThrownBy(() ->
                credentialService.updateCredential(credentialId, user.getId(), reuseRequest))
                .isInstanceOf(PasswordReuseException.class)
                .hasMessageContaining("cannot be reused");
    }

    @Test
    void softDeleteHidesFromActiveQueriesAndAppearsInTrash() {
        credentialService.deleteCredential(credentialId, user.getId());

        assertThatThrownBy(() ->
                credentialService.getCredentialById(credentialId, user.getId()))
                .isInstanceOf(CredentialNotFoundException.class);

        VaultQueryRequest query = new VaultQueryRequest();
        assertThat(credentialService.getCredentials(user.getId(), query).getContent())
                .isEmpty();

        List<CredentialResponse> trash =
                credentialService.getTrashCredentials(user.getId(), query).getContent();
        assertThat(trash).extracting(CredentialResponse::getId).contains(credentialId);
        assertThat(trash.get(0).isDeleted()).isTrue();
    }

    @Test
    void restoreBringsCredentialBackToActiveVault() {
        credentialService.deleteCredential(credentialId, user.getId());

        CredentialResponse restored =
                credentialService.restoreCredential(credentialId, user.getId());

        assertThat(restored.getId()).isEqualTo(credentialId);
        assertThat(restored.isDeleted()).isFalse();

        VaultQueryRequest query = new VaultQueryRequest();
        assertThat(credentialService.getCredentials(user.getId(), query).getContent())
                .extracting(CredentialResponse::getId)
                .contains(credentialId);
    }

    @Test
    void permanentDeleteRemovesCredentialAndPasswordHistory() {
        CredentialUpdateRequest updateRequest = new CredentialUpdateRequest();
        updateRequest.setPassword("NewPass2!");
        credentialService.updateCredential(credentialId, user.getId(), updateRequest);

        assertThat(passwordHistoryRepository.countByCredential_Id(credentialId)).isEqualTo(1);

        credentialService.deleteCredential(credentialId, user.getId());
        credentialService.permanentlyDeleteCredential(credentialId, user.getId());

        assertThat(credentialRepository.findById(credentialId)).isEmpty();
        assertThat(passwordHistoryRepository.countByCredential_Id(credentialId)).isZero();
    }
}
