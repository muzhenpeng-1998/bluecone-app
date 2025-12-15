package com.bluecone.app.config;

import com.bluecone.app.core.contextkit.CaffeineContextCache;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.TwoLevelContextCache;
import com.bluecone.app.core.contextkit.VersionChecker;
import com.bluecone.app.infra.contextkit.RedisContextCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * ContextMiddlewareKit 自动配置：提供通用缓存与版本校验组件。
 */
@Configuration
@ConditionalOnProperty(prefix = "bluecone.contextkit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ContextKitAutoConfiguration {

    @Bean(name = "contextKitL1Cache")
    @ConditionalOnMissingBean(name = "contextKitL1Cache")
    public ContextCache contextKitL1Cache(ContextKitProperties props) {
        return new CaffeineContextCache(100_000L);
    }

    @Bean(name = "contextKitL2Cache")
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    public ContextCache contextKitL2Cache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisContextCache(redisTemplate, objectMapper);
    }

    @Bean(name = "contextKitCache")
    @ConditionalOnBean(name = "contextKitL2Cache")
    public ContextCache twoLevelContextCache(ContextCache contextKitL1Cache,
                                             ContextCache contextKitL2Cache) {
        return new TwoLevelContextCache(contextKitL1Cache, contextKitL2Cache);
    }

    @Bean
    @ConditionalOnMissingBean(VersionChecker.class)
    public VersionChecker versionChecker(ContextKitProperties props) {
        return new VersionChecker(props.getVersionCheckWindow(), props.getVersionCheckSampleRate());
    }
}
