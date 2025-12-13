package com.bluecone.app.config;

import java.time.Clock;

import com.bluecone.app.core.create.api.IdempotentCreateTemplate;
import com.bluecone.app.core.create.application.DefaultIdempotentCreateTemplate;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.idempotency.api.IdempotencyTemplate;
import com.bluecone.app.core.idempotency.application.DefaultIdempotencyTemplate;
import com.bluecone.app.core.idempotency.spi.IdempotencyLock;
import com.bluecone.app.core.idempotency.spi.IdempotencyMetrics;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 幂等相关模板装配配置，仅负责将 app-core 的模板与 app-infra 的实现组装为 Bean。
 */
@Configuration
@ConditionalOnClass(ObjectMapper.class)
public class IdempotencyConfiguration {

    @Bean
    @ConditionalOnBean(IdempotencyRepository.class)
    public IdempotencyTemplate idempotencyTemplate(IdempotencyRepository repository,
                                                   ObjectMapper objectMapper) {
        IdempotencyLock lock = IdempotencyLock.noop();
        IdempotencyMetrics metrics = IdempotencyMetrics.noop();
        return new DefaultIdempotencyTemplate(repository, lock, metrics, objectMapper, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnBean({IdService.class, PublicIdCodec.class, IdempotencyRepository.class, PlatformTransactionManager.class})
    @ConditionalOnProperty(prefix = "bluecone.create", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IdempotentCreateTemplate idempotentCreateTemplate(IdService idService,
                                                             PublicIdCodec publicIdCodec,
                                                             IdempotencyRepository repository,
                                                             PlatformTransactionManager transactionManager,
                                                             ObjectMapper objectMapper,
                                                             DomainEventPublisher domainEventPublisher) {
        return new DefaultIdempotentCreateTemplate(
                idService,
                publicIdCodec,
                repository,
                transactionManager,
                objectMapper,
                Clock.systemUTC(),
                domainEventPublisher
        );
    }
}
