package com.example.securevault.service;

import com.example.securevault.dto.CredentialRequest;
import com.example.securevault.dto.LoginRequest;
import com.example.securevault.dto.VaultQueryRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import com.example.securevault.entity.User;
import com.example.securevault.repository.AuditLogRepository;
import com.example.securevault.repository.CategoryRepository;
import com.example.securevault.repository.CredentialRepository;
import com.example.securevault.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Measures Hibernate prepare-statement counts for vault read/write paths.
 * Timings are printed for documentation in docs/PERFORMANCE.md.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class QueryCountBenchmarkTest {

    private static final int CREDENTIAL_COUNT = 50;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User user;
    private Long sampleCredentialId;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        credentialRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User(
                "Benchmark User",
                "benchmark@securevault.test",
                passwordEncoder.encode("BenchmarkPass1")
        ));

        for (int i = 0; i < CREDENTIAL_COUNT; i++) {
            CredentialRequest request = new CredentialRequest();
            request.setTitle("Credential " + String.format("%02d", i));
            request.setUsername("user" + i + "@example.com");
            request.setPassword("SecretPass" + i);
            request.setCategory("CAT-" + i);
            request.setWebsiteUrl("https://example.com/" + i);
            credentialService.createCredential(user.getId(), request);
        }

        sampleCredentialId = credentialRepository.findByUserIdAndDeletedFalse(user.getId())
                .get(0)
                .getId();

        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    void benchmarkQueryCountsAndTimings() {
        long unoptimizedCount = measurePrepareStatements(() ->
                credentialService.getAllCredentialsUnoptimized(user.getId()));
        System.out.printf("getAllCredentialsUnoptimized: %d statements, %d ms%n",
                unoptimizedCount, lastElapsedMs);
        assertThat(unoptimizedCount).isGreaterThan(50);

        long optimizedCount = measurePrepareStatements(() ->
                credentialService.getAllCredentialsOptimized(user.getId()));
        System.out.printf("getAllCredentialsOptimized: %d statements, %d ms%n",
                optimizedCount, lastElapsedMs);
        assertThat(optimizedCount).isLessThanOrEqualTo(3);

        VaultQueryRequest pageQuery = new VaultQueryRequest();
        pageQuery.setPage(0);
        pageQuery.setSize(10);
        long pagedCount = measurePrepareStatements(() ->
                credentialService.getCredentials(user.getId(), pageQuery));
        System.out.printf("getCredentials(page=0,size=10): %d statements, %d ms%n",
                pagedCount, lastElapsedMs);
        assertThat(pagedCount).isLessThanOrEqualTo(4);

        VaultQueryRequest searchQuery = new VaultQueryRequest();
        searchQuery.setTitle("Credential 0");
        searchQuery.setSize(10);
        long searchCount = measurePrepareStatements(() ->
                credentialService.getCredentials(user.getId(), searchQuery));
        System.out.printf("getCredentials(search title): %d statements, %d ms%n",
                searchCount, lastElapsedMs);
        assertThat(searchCount).isLessThanOrEqualTo(4);

        long byIdCount = measurePrepareStatements(() ->
                credentialService.getCredentialById(sampleCredentialId, user.getId()));
        System.out.printf("getCredentialById: %d statements, %d ms%n",
                byIdCount, lastElapsedMs);
        assertThat(byIdCount).isLessThanOrEqualTo(3);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(user.getEmail());
        loginRequest.setPassword("BenchmarkPass1");
        long loginCount = measurePrepareStatements(() ->
                userService.loginUser(loginRequest));
        System.out.printf("userService.loginUser: %d statements, %d ms%n",
                loginCount, lastElapsedMs);
        assertThat(loginCount).isLessThanOrEqualTo(2);

        CredentialRequest createRequest = new CredentialRequest();
        createRequest.setTitle("New Benchmark Entry");
        createRequest.setUsername("new@example.com");
        createRequest.setPassword("NewSecret1");
        createRequest.setCategory("WORK");
        createRequest.setWebsiteUrl("https://new.example.com");
        long createCount = measurePrepareStatements(() ->
                credentialService.createCredential(user.getId(), createRequest));
        System.out.printf("createCredential: %d statements, %d ms%n",
                createCount, lastElapsedMs);
        assertThat(createCount).isGreaterThan(0);
        assertThat(createCount).isLessThanOrEqualTo(10);
    }

    private long lastElapsedMs;

    private long measurePrepareStatements(Runnable operation) {
        entityManager.clear();
        statistics.clear();
        long start = System.nanoTime();
        operation.run();
        long elapsedNs = System.nanoTime() - start;
        lastElapsedMs = elapsedNs / 1_000_000;
        return statistics.getPrepareStatementCount();
    }
}
