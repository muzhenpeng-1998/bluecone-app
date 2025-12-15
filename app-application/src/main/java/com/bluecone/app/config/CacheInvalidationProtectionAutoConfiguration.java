package com.bluecone.app.config;

import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationPublisher;
import com.bluecone.app.core.cacheinval.application.CacheInvalidationExecutor;
import com.bluecone.app.core.cacheinval.application.DefaultCacheInvalidationPublisher;
import com.bluecone.app.core.cacheinval.coalesce.DefaultInvalidationCoalescer;
import com.bluecone.app.core.cacheinval.coalesce.InvalidationCoalescer;
import com.bluecone.app.core.cacheinval.guard.DefaultInvalidationStormGuard;
import com.bluecone.app.core.cacheinval.guard.InvalidationStormGuard;
import com.bluecone.app.core.cacheinval.transport.CacheInvalidationBus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Auto-configuration for cache invalidation protection (storm guard + coalescer + epoch bump).
 */
@Configuration
@ConditionalOnProperty(prefix = "bluecone.cache.invalidation.protection", name = "enabled", havingValue = "true")
public class CacheInvalidationProtectionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(InvalidationStormGuard.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    public InvalidationStormGuard redisStormGuard(CacheInvalidationProtectionProperties props,
                                                  StringRedisTemplate redisTemplate) {
        return new DefaultInvalidationStormGuard(
                props.getCoalesceThresholdPerMinute(),
                props.getStormThresholdPerMinute(),
                props.getStormCooldown(),
                props.getMaxKeysPerEvent(),
                redisTemplate,
                props.isRedisStormEnabled()
        );
    }

    @Bean
    @ConditionalOnMissingBean(InvalidationStormGuard.class)
    public InvalidationStormGuard localStormGuard(CacheInvalidationProtectionProperties props) {
        return new DefaultInvalidationStormGuard(
                props.getCoalesceThresholdPerMinute(),
                props.getStormThresholdPerMinute(),
                props.getStormCooldown(),
                props.getMaxKeysPerEvent(),
                null,
                false
        );
    }

    @Bean
    @ConditionalOnMissingBean(InvalidationCoalescer.class)
    public InvalidationCoalescer invalidationCoalescer(CacheInvalidationProtectionProperties props,
                                                       CacheInvalidationPublisher publisher) {
        return new DefaultInvalidationCoalescer(
                publisher,
                props.getDebounceWindow(),
                props.getMaxKeysPerBatch(),
                props.getMaxKeysPerEvent()
        );
    }

    @Bean
    @ConditionalOnMissingBean(name = "protectedCacheInvalidationPublisher")
    public CacheInvalidationPublisher protectedCacheInvalidationPublisher(CacheInvalidationExecutor executor,
                                                                          CacheInvalidationBus bus,
                                                                          InvalidationStormGuard stormGuard,
                                                                          InvalidationCoalescer coalescer,
                                                                          CacheEpochProvider epochProvider,
                                                                          CacheInvalidationProperties baseProps) {
        DefaultCacheInvalidationPublisher delegate = new DefaultCacheInvalidationPublisher(executor, bus);
        return new com.bluecone.app.core.cacheinval.application.ProtectedCacheInvalidationPublisher(
                delegate,
                stormGuard,
                coalescer,
                epochProvider,
                baseProps.getMaxKeysPerEvent()
        );
    }
}

