package com.bluecone.app.config;

import com.bluecone.app.application.middleware.StoreContextResolver;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.VersionChecker;
import com.bluecone.app.core.idresolve.api.PublicIdResolver;
import com.bluecone.app.store.runtime.application.StoreSnapshotProvider;
import com.bluecone.app.store.runtime.spi.StoreSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 门店上下文中间件自动配置。
 */
@Configuration
@ConditionalOnProperty(prefix = "bluecone.store.context", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StoreContextAutoConfiguration {

    @Bean
    @ConditionalOnBean({StoreSnapshotRepository.class, ObjectMapper.class, ContextCache.class})
    public StoreSnapshotProvider storeSnapshotProvider(StoreSnapshotRepository repository,
                                                       @Qualifier("contextKitCache") ContextCache contextCache,
                                                       VersionChecker versionChecker,
                                                       ContextKitProperties kitProperties,
                                                       ObjectMapper objectMapper) {
        return new StoreSnapshotProvider(
                repository,
                contextCache,
                versionChecker,
                kitProperties,
                objectMapper
        );
    }

    @Bean
    @ConditionalOnBean({PublicIdResolver.class, StoreSnapshotProvider.class})
    public StoreContextResolver storeContextResolver(PublicIdResolver publicIdResolver,
                                                     StoreSnapshotProvider provider,
                                                     StoreContextProperties props) {
        return new StoreContextResolver(publicIdResolver, provider, props);
    }
}
