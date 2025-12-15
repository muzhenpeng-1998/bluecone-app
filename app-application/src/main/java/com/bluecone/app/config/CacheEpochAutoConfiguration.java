package com.bluecone.app.config;

import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.cacheepoch.application.DefaultCacheEpochProvider;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Auto-configuration for {@link CacheEpochProvider}.
 *
 * <p>This configuration is unconditional so that ContextMiddlewareKit can always
 * rely on epoch-based cache keying. When Redis is available and enabled via
 * {@link CacheInvalidationProtectionProperties#isRedisEpochEnabled()}, the
 * provider will use Redis as L2; otherwise it falls back to single-node mode.</p>
 */
@Configuration
public class CacheEpochAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheEpochProvider.class)
    @ConditionalOnClass(StringRedisTemplate.class)
    public CacheEpochProvider redisAwareCacheEpochProvider(CacheInvalidationProtectionProperties protectionProps,
                                                           StringRedisTemplate redisTemplate) {
        Duration l1Ttl = protectionProps.getEpochL1Ttl();
        if (!protectionProps.isRedisEpochEnabled()) {
            return new DefaultCacheEpochProvider(l1Ttl);
        }
        Duration redisTtl = null;
        return new DefaultCacheEpochProvider(l1Ttl, redisTemplate, true, redisTtl);
    }

    @Bean
    @ConditionalOnMissingBean(CacheEpochProvider.class)
    public CacheEpochProvider localCacheEpochProvider(CacheInvalidationProtectionProperties protectionProps) {
        return new DefaultCacheEpochProvider(protectionProps.getEpochL1Ttl());
    }
}

