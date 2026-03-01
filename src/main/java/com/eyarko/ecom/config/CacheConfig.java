package com.eyarko.ecom.config;

import com.eyarko.ecom.infrastructure.web.IdempotencyEntry;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

    @Bean
    public Cache<String, IdempotencyEntry> idempotencyCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();
    }
}

