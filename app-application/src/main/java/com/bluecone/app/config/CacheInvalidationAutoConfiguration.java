package com.bluecone.app.config;

import com.bluecone.app.cacheinval.CacheInvalidationListener;
import com.bluecone.app.cacheinval.transport.InProcessCacheInvalidationBus;
import com.bluecone.app.cacheinval.transport.OutboxCacheInvalidationBus;
import com.bluecone.app.cacheinval.transport.RedisPubSubCacheInvalidationBus;
import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationPublisher;
import com.bluecone.app.core.cacheinval.application.CacheInvalidationExecutor;
import com.bluecone.app.core.cacheinval.application.DefaultCacheInvalidationExecutor;
import com.bluecone.app.core.cacheinval.application.DefaultCacheInvalidationPublisher;
import com.bluecone.app.core.cacheinval.observability.api.CacheInvalidationLogWriter;
import com.bluecone.app.core.cacheinval.transport.CacheInvalidationBus;
import com.bluecone.app.core.cacheinval.transport.InvalidationTransport;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Auto-configuration for cache invalidation events.
 */
@Configuration
@ConditionalOnProperty(prefix = "bluecone.cache.invalidation", name = "enabled", havingValue = "true")
public class CacheInvalidationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheInvalidationExecutor.class)
    public CacheInvalidationExecutor cacheInvalidationExecutor(ContextCache contextKitCache,
                                                               CacheEpochProvider cacheEpochProvider) {
        return new DefaultCacheInvalidationExecutor(contextKitCache, cacheEpochProvider);
    }

    @Bean
    @ConditionalOnMissingBean(CacheInvalidationBus.class)
    public CacheInvalidationBus cacheInvalidationBus(CacheInvalidationProperties properties,
                                                     ContextCache contextKitCache,
                                                     ObjectMapper objectMapper,
                                                     org.springframework.context.ApplicationContext applicationContext) {
        InvalidationTransport transport = properties.getTransport();
        if (transport == InvalidationTransport.OUTBOX || transport == null) {
            DomainEventPublisher domainEventPublisher = applicationContext.getBean(DomainEventPublisher.class);
            return new OutboxCacheInvalidationBus(domainEventPublisher);
        }
        if (transport == InvalidationTransport.REDIS_PUBSUB) {
            StringRedisTemplate redisTemplate = applicationContext.getBean(StringRedisTemplate.class);
            RedisPubSubCacheInvalidationBus bus = new RedisPubSubCacheInvalidationBus(redisTemplate, objectMapper, properties);
            RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
            if (factory != null) {
                RedisMessageListenerContainer container = new RedisMessageListenerContainer();
                container.setConnectionFactory(factory);
                container.addMessageListener(bus, new PatternTopic(properties.getRedisTopic()));
                container.afterPropertiesSet();
                container.start();
            }
            return bus;
        }
        return new InProcessCacheInvalidationBus();
    }

    @Bean
    @ConditionalOnMissingBean(CacheInvalidationPublisher.class)
    public CacheInvalidationPublisher cacheInvalidationPublisher(CacheInvalidationExecutor executor,
                                                                 CacheInvalidationBus bus) {
        return new DefaultCacheInvalidationPublisher(executor, bus);
    }

    @Bean
    public CacheInvalidationListener cacheInvalidationListener(CacheInvalidationExecutor executor,
                                                               CacheInvalidationBus bus,
                                                               CacheInvalidationProperties properties,
                                                               CacheInvalidationLogWriter logWriter,
                                                               @Value("${bluecone.instance.id:}") String instanceId,
                                                               CacheEpochProvider cacheEpochProvider) {
        String resolvedInstanceId = resolveInstanceId(instanceId);
        String transportName = properties.getTransport() != null
                ? properties.getTransport().name()
                : InvalidationTransport.OUTBOX.name();
        return new CacheInvalidationListener(executor, bus, properties, logWriter, resolvedInstanceId, transportName, cacheEpochProvider);
    }

    private String resolveInstanceId(String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
