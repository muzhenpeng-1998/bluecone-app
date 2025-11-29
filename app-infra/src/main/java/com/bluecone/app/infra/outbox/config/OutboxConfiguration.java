// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/config/OutboxConfiguration.java
package com.bluecone.app.infra.outbox.config;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.infra.outbox.core.DefaultEventSerializer;
import com.bluecone.app.infra.outbox.core.EventConsumptionTracker;
import com.bluecone.app.infra.outbox.core.EventSerializer;
import com.bluecone.app.infra.outbox.core.RedisEventConsumptionTracker;
import com.bluecone.app.infra.outbox.core.RetryPolicy;
import com.bluecone.app.infra.outbox.core.SimpleExponentialBackoffRetryPolicy;
import com.bluecone.app.infra.outbox.core.TransactionalOutboxEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Outbox 自动装配：注册序列化器、开启定时任务。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventSerializer.class)
    public EventSerializer eventSerializer(final ObjectMapper objectMapper) {
        return new DefaultEventSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(RetryPolicy.class)
    public RetryPolicy retryPolicy(final OutboxProperties properties) {
        return new SimpleExponentialBackoffRetryPolicy(properties);
    }

    @Bean
    @ConditionalOnMissingBean(EventConsumptionTracker.class)
    public EventConsumptionTracker eventConsumptionTracker(final RedisEventConsumptionTracker tracker) {
        return tracker;
    }

    /**
     * 生产/准生产：事务 Outbox 写库，高可靠异步投递。
     */
    @Bean
    @Profile({"!dev", "!test"})
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public DomainEventPublisher prodDomainEventPublisher(final TransactionalOutboxEventPublisher publisher) {
        return publisher;
    }

}
