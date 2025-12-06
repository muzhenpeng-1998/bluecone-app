package com.bluecone.app.infra.integration.config;

import com.bluecone.app.infra.integration.channel.IntegrationChannel;
import com.bluecone.app.infra.integration.domain.IntegrationChannelType;
import com.bluecone.app.infra.integration.support.IntegrationHttpClient;
import com.bluecone.app.infra.integration.support.IntegrationKeyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Integration Hub 自动装配，装配核心 Bean 与通道注册表。
 */
@Configuration
@EnableConfigurationProperties(IntegrationProperties.class)
@ConditionalOnProperty(prefix = "bluecone.integration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationConfiguration {

    private static final Logger log = LoggerFactory.getLogger(IntegrationConfiguration.class);

    @Bean
    public IntegrationHttpClient integrationHttpClient(final RestTemplateBuilder restTemplateBuilder) {
        return new IntegrationHttpClient(restTemplateBuilder);
    }

    @Bean
    public IntegrationKeyBuilder integrationKeyBuilder() {
        return new IntegrationKeyBuilder();
    }

    /**
     * 将所有通道注册到 Map，方便按类型检索。
     */
    @Bean
    public Map<IntegrationChannelType, IntegrationChannel> integrationChannelRegistry(final List<IntegrationChannel> channels) {
        Objects.requireNonNull(channels, "channels must not be null");
        Map<IntegrationChannelType, IntegrationChannel> map = new EnumMap<>(IntegrationChannelType.class);
        for (IntegrationChannel channel : channels) {
            map.put(channel.type(), channel);
            log.info("[Integration] register channel={} ({})", channel.type(), channel.getClass().getSimpleName());
        }
        return map;
    }
}
