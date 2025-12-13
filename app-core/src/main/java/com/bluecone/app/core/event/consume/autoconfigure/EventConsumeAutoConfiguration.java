package com.bluecone.app.core.event.consume.autoconfigure;

import com.bluecone.app.core.event.consume.api.EventHandlerTemplate;
import com.bluecone.app.core.event.consume.application.DefaultEventHandlerTemplate;
import com.bluecone.app.core.event.consume.spi.ConsumeMetrics;
import com.bluecone.app.core.event.consume.spi.EventDedupRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

/**
 * 事件消费幂等模板的自动装配。
 */
@AutoConfiguration
@ConditionalOnBean(EventDedupRepository.class)
@ConditionalOnClass(PlatformTransactionManager.class)
@ConditionalOnProperty(prefix = "bluecone.eventing.consume", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventConsumeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventHandlerTemplate.class)
    public EventHandlerTemplate eventHandlerTemplate(EventDedupRepository repository,
                                                     PlatformTransactionManager transactionManager) {
        return new DefaultEventHandlerTemplate(
                repository,
                transactionManager,
                Clock.systemUTC(),
                ConsumeMetrics.noop()
        );
    }
}

