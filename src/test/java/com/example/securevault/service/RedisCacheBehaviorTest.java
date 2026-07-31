package com.example.securevault.service;

import com.example.securevault.config.CacheNames;
import com.example.securevault.dto.CategoryUpdateRequest;
import com.example.securevault.dto.CredentialRequest;
import com.example.securevault.dto.CredentialResponse;
import com.example.securevault.dto.CredentialUpdateRequest;
import com.example.securevault.dto.UserResponse;
import com.example.securevault.entity.User;
import com.example.securevault.repository.AuditLogRepository;
import com.example.securevault.repository.CategoryRepository;
import com.example.securevault.repository.CredentialRepository;
import com.example.securevault.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates cache miss → hit (no extra SQL) and eviction / refresh after updates.
 * Uses in-memory Spring Cache ({@code securevault.cache.redis-enabled=false}).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class RedisCacheBehaviorTest {

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private CategoryService categoryService;

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
    private CacheManager cacheManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User user;
    private Long credentialId;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        credentialRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        clearAllCaches();

        user = userRepository.save(new User(
                "Cache User",
                "cache-user@securevault.test",
                passwordEncoder.encode("CachePass1")
        ));

        CredentialRequest create = new CredentialRequest();
        create.setTitle("Cached Title");
        create.setUsername("cache@example.com");
        create.setPassword("SecretPass1");
        create.setCategory("WORK");
        create.setWebsiteUrl("https://example.com");

        credentialId = credentialService.createCredential(user.getId(), create).getId();

        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        entityManager.clear();
    }

    @Test
    void credentialDetails_cacheMissThenHit_avoidsSecondDbQuery() {
        CredentialResponse first = credentialService.getCredentialById(credentialId, user.getId());
        long afterMiss = statistics.getPrepareStatementCount();
        assertThat(afterMiss).isGreaterThan(0);
        assertThat(first.getTitle()).isEqualTo("Cached Title");

        statistics.clear();
        entityManager.clear();

        CredentialResponse second = credentialService.getCredentialById(credentialId, user.getId());
        long afterHit = statistics.getPrepareStatementCount();

        assertThat(afterHit).isZero();
        assertThat(second.getTitle()).isEqualTo("Cached Title");
        assertThat(cacheManager.getCache(CacheNames.CREDENTIALS)
                .get(credentialId + "-" + user.getId())).isNotNull();
    }

    @Test
    void credentialUpdate_refreshesCache_soStaleDataIsNotReturned() {
        credentialService.getCredentialById(credentialId, user.getId());

        CredentialUpdateRequest update = new CredentialUpdateRequest();
        update.setTitle("Fresh Title After Update");
        credentialService.updateCredential(credentialId, user.getId(), update);

        entityManager.clear();
        statistics.clear();

        CredentialResponse afterUpdate =
                credentialService.getCredentialById(credentialId, user.getId());

        // @CachePut stored fresh value — no DB round-trip required
        assertThat(statistics.getPrepareStatementCount()).isZero();
        assertThat(afterUpdate.getTitle()).isEqualTo("Fresh Title After Update");
    }

    @Test
    void credentialDelete_evictsCache() {
        credentialService.getCredentialById(credentialId, user.getId());
        assertThat(cacheManager.getCache(CacheNames.CREDENTIALS)
                .get(credentialId + "-" + user.getId())).isNotNull();

        credentialService.deleteCredential(credentialId, user.getId());

        assertThat(cacheManager.getCache(CacheNames.CREDENTIALS)
                .get(credentialId + "-" + user.getId())).isNull();
    }

    @Test
    void userProfile_cacheMissThenHit() {
        UserResponse first = userService.getUserProfile(user.getId());
        long afterMiss = statistics.getPrepareStatementCount();
        assertThat(afterMiss).isGreaterThan(0);
        assertThat(first.getEmail()).isEqualTo("cache-user@securevault.test");

        statistics.clear();
        entityManager.clear();

        userService.getUserProfile(user.getId());
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    @Test
    void categories_cacheMissHit_andEvictOnUpdate() {
        var list1 = categoryService.getCategoriesForUser(user.getId());
        assertThat(list1).isNotEmpty();
        long afterMiss = statistics.getPrepareStatementCount();
        assertThat(afterMiss).isGreaterThan(0);

        statistics.clear();
        entityManager.clear();

        categoryService.getCategoriesForUser(user.getId());
        assertThat(statistics.getPrepareStatementCount()).isZero();

        Long categoryId = list1.get(0).getId();
        CategoryUpdateRequest rename = new CategoryUpdateRequest();
        rename.setName("RENAMED-WORK");
        categoryService.updateCategory(user.getId(), categoryId, rename);

        assertThat(cacheManager.getCache(CacheNames.CATEGORIES).get(user.getId())).isNull();

        statistics.clear();
        entityManager.clear();

        var list2 = categoryService.getCategoriesForUser(user.getId());
        assertThat(list2.get(0).getName()).isEqualTo("RENAMED-WORK");
        assertThat(statistics.getPrepareStatementCount()).isGreaterThan(0);
    }

    private void clearAllCaches() {
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
