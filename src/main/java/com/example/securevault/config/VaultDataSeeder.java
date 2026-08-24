package com.example.securevault.config;

import com.example.securevault.entity.Category;
import com.example.securevault.entity.Credential;
import com.example.securevault.entity.User;
import com.example.securevault.repository.CategoryRepository;
import com.example.securevault.repository.CredentialRepository;
import com.example.securevault.repository.UserRepository;
import com.example.securevault.util.AESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class VaultDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VaultDataSeeder.class);
    private static final String DEMO_EMAIL = "demo@securevault.local";
    private static final String DEMO_PASSWORD = "DemoPass1";
    private static final int TARGET_CREDENTIALS = 50;

    private static final String[] CATEGORIES = {
            "DEVELOPMENT", "FINANCE", "SOCIAL", "WORK", "PERSONAL", "SHOPPING"
    };

    private static final String[] SITES = {
            "github.com", "gitlab.com", "bitbucket.org", "aws.amazon.com",
            "azure.microsoft.com", "cloud.google.com", "stackoverflow.com",
            "linkedin.com", "twitter.com", "facebook.com", "netflix.com",
            "spotify.com", "amazon.com", "ebay.com", "paypal.com",
            "bankofamerica.com", "chase.com", "gmail.com", "outlook.com",
            "dropbox.com", "notion.so", "figma.com", "slack.com", "discord.com",
            "reddit.com"
    };

    private final boolean seedEnabled;
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final CategoryRepository categoryRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AESUtil aesUtil;

    public VaultDataSeeder(
            @Value("${securevault.seed.enabled:false}") boolean seedEnabled,
            UserRepository userRepository,
            CredentialRepository credentialRepository,
            CategoryRepository categoryRepository,
            BCryptPasswordEncoder passwordEncoder,
            AESUtil aesUtil) {
        this.seedEnabled = seedEnabled;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.aesUtil = aesUtil;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        User demoUser = userRepository.findByEmail(DEMO_EMAIL)
                .orElseGet(this::createDemoUser);

        long existing = credentialRepository.countByUserIdAndDeletedFalse(demoUser.getId());
        if (existing >= TARGET_CREDENTIALS) {
            log.info("Seed skipped: demo user already has {} credentials", existing);
            return;
        }

        Category[] categories = new Category[CATEGORIES.length];
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int idx = i;
            categories[i] = categoryRepository
                    .findByUserIdAndNameIgnoreCase(demoUser.getId(), CATEGORIES[i])
                    .orElseGet(() -> categoryRepository.save(
                            new Category(CATEGORIES[idx], CATEGORIES[idx] + " vault items", demoUser)
                    ));
        }

        int toCreate = TARGET_CREDENTIALS - (int) existing;
        for (int i = 1; i <= toCreate; i++) {
            int index = (int) existing + i;
            Credential credential = new Credential();
            credential.setTitle("Credential " + index);
            credential.setUsername("user" + index + "@example.com");
            credential.setEncryptedPassword(aesUtil.encrypt("SeedPassword!" + index));
            credential.setWebsiteUrl("https://www." + SITES[(index - 1) % SITES.length]);
            credential.setCategory(categories[(index - 1) % categories.length]);
            credential.setNotes("Seeded credential #" + index + " for pagination testing");
            credential.setUser(demoUser);
            credential.setDeleted(false);
            credentialRepository.save(credential);
        }

        log.info(
                "Seeded {} credentials for {} (login: {} / {})",
                toCreate,
                DEMO_EMAIL,
                DEMO_EMAIL,
                DEMO_PASSWORD
        );
    }

    private User createDemoUser() {
        return userRepository.save(new User(
                "Demo User",
                DEMO_EMAIL,
                passwordEncoder.encode(DEMO_PASSWORD)
        ));
    }
}
