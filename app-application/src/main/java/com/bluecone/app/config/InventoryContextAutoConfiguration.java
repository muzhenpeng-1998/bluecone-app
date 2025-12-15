package com.bluecone.app.config;

import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.VersionChecker;
import com.bluecone.app.application.middleware.InventoryContextResolver;
import com.bluecone.app.gateway.middleware.InventoryMiddleware;
import com.bluecone.app.inventory.runtime.application.InventoryPolicySnapshotProvider;
import com.bluecone.app.inventory.runtime.spi.InventoryPolicyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 库存上下文中间件自动配置。
 */
@Configuration
@ConditionalOnProperty(prefix = "bluecone.inventory.context", name = "enabled", havingValue = "true")
public class InventoryContextAutoConfiguration {

    @Bean
    @ConditionalOnBean({InventoryPolicyRepository.class, ObjectMapper.class, ContextCache.class})
    public InventoryPolicySnapshotProvider inventoryPolicySnapshotProvider(InventoryPolicyRepository repository,
                                                                           @Qualifier("contextKitCache") ContextCache contextCache,
                                                                           VersionChecker versionChecker,
                                                                           ContextKitProperties kitProperties,
                                                                           ObjectMapper objectMapper,
                                                                           com.bluecone.app.core.cacheepoch.api.CacheEpochProvider cacheEpochProvider) {
        return new InventoryPolicySnapshotProvider(
                repository,
                contextCache,
                versionChecker,
                kitProperties,
                objectMapper,
                cacheEpochProvider
        );
    }

    @Bean
    @ConditionalOnBean(InventoryPolicySnapshotProvider.class)
    public InventoryContextResolver inventoryContextResolver(InventoryPolicySnapshotProvider provider,
                                                             InventoryContextProperties props) {
        return new InventoryContextResolver(provider, props);
    }

    @Bean
    @ConditionalOnBean(InventoryContextResolver.class)
    public InventoryMiddleware inventoryMiddleware(InventoryContextResolver resolver,
                                                   InventoryContextProperties props) {
        return new InventoryMiddleware(resolver, props);
    }
}
