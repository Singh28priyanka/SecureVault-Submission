package com.example.securevault.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory Spring Cache for tests / environments without Redis.
 * Activate with {@code securevault.cache.redis-enabled=false}.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "securevault.cache.redis-enabled", havingValue = "false")
public class SimpleCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CacheNames.USERS,
                CacheNames.CREDENTIALS,
                CacheNames.CATEGORIES
        );
    }
}
