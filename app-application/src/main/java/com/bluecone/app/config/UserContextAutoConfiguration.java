package com.bluecone.app.config;

import com.bluecone.app.application.middleware.UserContextResolver;
import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.VersionChecker;
import com.bluecone.app.core.user.runtime.spi.UserPrincipalResolver;
import com.bluecone.app.core.user.runtime.spi.UserSnapshotRepository;
import com.bluecone.app.gateway.middleware.UserMiddleware;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用户上下文中间件自动配置。
 */
@Configuration
@ConditionalOnProperty(prefix = "bluecone.user.context", name = "enabled", havingValue = "true")
public class UserContextAutoConfiguration {

    @Bean
    @ConditionalOnBean({UserPrincipalResolver.class, UserSnapshotRepository.class, ObjectMapper.class, ContextCache.class})
    public UserContextResolver userContextResolver(UserPrincipalResolver principalResolver,
                                                   UserSnapshotRepository snapshotRepository,
                                                   @Qualifier("contextKitCache") ContextCache contextCache,
                                                   VersionChecker versionChecker,
                                                   ContextKitProperties kitProperties,
                                                   UserContextProperties props,
                                                   ObjectMapper objectMapper,
                                                   CacheEpochProvider cacheEpochProvider) {
        return new UserContextResolver(
                principalResolver,
                snapshotRepository,
                contextCache,
                versionChecker,
                kitProperties,
                props,
                objectMapper,
                cacheEpochProvider
        );
    }

    @Bean
    @ConditionalOnBean(UserContextResolver.class)
    public UserMiddleware userMiddleware(UserContextResolver resolver,
                                         UserContextProperties props) {
        return new UserMiddleware(resolver, props);
    }
}
