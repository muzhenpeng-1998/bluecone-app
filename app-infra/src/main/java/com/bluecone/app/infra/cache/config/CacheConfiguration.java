package com.bluecone.app.infra.cache.config;

import com.bluecone.app.infra.cache.consistency.ConsistencyBus;
import com.bluecone.app.infra.cache.consistency.RedisConsistencyBus;
import com.bluecone.app.infra.cache.consistency.NoopConsistencyBus;
import com.bluecone.app.infra.cache.core.CacheEngine;
import com.bluecone.app.infra.cache.core.CacheStore;
import com.bluecone.app.infra.cache.core.L1CacheStore;
import com.bluecone.app.infra.cache.core.L2CacheStore;
import com.bluecone.app.infra.cache.core.NoopCacheStore;
import com.bluecone.app.infra.cache.facade.CacheClient;
import com.bluecone.app.infra.cache.profile.CacheProfileRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存核心 Bean 定义，按“三层模型”装配。
 */
@Configuration
public class CacheConfiguration {

    @Bean
    public L1CacheStore l1CacheStore(@Value("${bluecone.cache.l1.max-size:10000}") long maximumSize) {
        return new L1CacheStore(maximumSize);
    }

    @ConditionalOnProperty(value = "bluecone.cache.l2.enabled", havingValue = "true", matchIfMissing = true)
    @Bean(name = "l2CacheStoreBean")
    public L2CacheStore l2CacheStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        return new L2CacheStore(stringRedisTemplate, objectMapper);
    }

    @ConditionalOnMissingBean(name = "l2CacheStoreBean")
    @Bean(name = "l2CacheStoreBean")
    public NoopCacheStore noopCacheStore() {
        return new NoopCacheStore();
    }

    @Bean
    @ConditionalOnProperty(value = "bluecone.cache.consistency.redis.enabled", havingValue = "true", matchIfMissing = true)
    public ConsistencyBus consistencyBus(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        return new RedisConsistencyBus(stringRedisTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(ConsistencyBus.class)
    public ConsistencyBus noopConsistencyBus() {
        return new NoopConsistencyBus();
    }

    @Bean
    public CacheEngine cacheEngine(L1CacheStore l1CacheStore,
                                   @Qualifier("l2CacheStoreBean") CacheStore l2CacheStore,
                                   ConsistencyBus consistencyBus) {
        return new CacheEngine(l1CacheStore, l2CacheStore, consistencyBus);
    }

    @Bean
    public CacheClient cacheClient(CacheEngine cacheEngine, CacheProfileRegistry cacheProfileRegistry) {
        return new CacheClient(cacheEngine, cacheProfileRegistry);
    }
}
