package com.bluecone.app.id.config;

import java.time.Clock;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.core.UlidIdGenerator;
import com.bluecone.app.id.core.UlidIdService;
import com.bluecone.app.id.metrics.UlidMetrics;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * 旧版 ID 配置类，已迁移至基于 AutoConfiguration 的 IdAutoConfiguration。
 *
 * <p>仅在显式开启 {@code bluecone.id.legacy-config-enabled=true} 时才会生效。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BlueconeIdProperties.class)
@ConditionalOnProperty(prefix = "bluecone.id", name = "legacy-config-enabled", havingValue = "true", matchIfMissing = false)
@Deprecated(forRemoval = false)
public class IdConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UlidIdGenerator ulidIdGenerator(BlueconeIdProperties props,
                                           ObjectProvider<Clock> clockProvider,
                                           ObjectProvider<MeterRegistry> meterRegistryProvider) {
        int stripes;
        BlueconeIdProperties.Mode mode = BlueconeIdProperties.Mode.STRIPED;
        BlueconeIdProperties.Ulid ulidProps = null;

        // 若整体未启用，则回退为严格单序模式（1 个分片），避免返回 null 造成 NPE
        if (props == null || !props.isEnabled()) {
            stripes = 1;
            mode = BlueconeIdProperties.Mode.STRICT;
        } else {
            ulidProps = props.getUlid();
            if (ulidProps == null) {
                stripes = 1;
                mode = BlueconeIdProperties.Mode.STRICT;
            } else if (ulidProps.getMode() == BlueconeIdProperties.Mode.STRICT) {
                // STRICT 模式强制为单分片
                stripes = 1;
                mode = BlueconeIdProperties.Mode.STRICT;
            } else {
                // STRIPED 模式使用配置的分片数量
                stripes = ulidProps.getStripes();
                mode = ulidProps.getMode();
            }
        }

        // 边界修正：至少 1 条带，最多 1024 条带
        if (stripes < 1) {
            stripes = 1;
        } else if (stripes > 1024) {
            stripes = 1024;
        }

        Clock clock = clockProvider.getIfAvailable();
        if (clock == null) {
            clock = Clock.systemUTC();
        }

        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        UlidMetrics metrics;
        if (ulidProps != null && ulidProps.isMetricsEnabled() && registry != null) {
            metrics = UlidMetrics.from(registry, "bluecone.id.ulid", stripes, mode);
        } else {
            metrics = UlidMetrics.noop();
        }

        BlueconeIdProperties.Ulid.Rollback rollback = (ulidProps != null ? ulidProps.getRollback() : null);

        return UlidIdGenerator.create(stripes, clock, rollback, metrics);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdService idService(UlidIdGenerator ulidIdGenerator) {
        return new UlidIdService(ulidIdGenerator);
    }
}
