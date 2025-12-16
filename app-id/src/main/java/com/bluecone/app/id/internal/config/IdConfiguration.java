package com.bluecone.app.id.internal.config;

import java.time.Clock;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.internal.config.BlueconeIdProperties.LongId;
import com.bluecone.app.id.internal.config.InstanceNodeIdProvider;
import com.bluecone.app.id.internal.core.PublicIdFactory;
import com.bluecone.app.id.internal.core.SnowflakeLongIdGenerator;
import com.bluecone.app.id.internal.core.UlidIdGenerator;
import com.bluecone.app.id.internal.core.UlidIdService;
import com.bluecone.app.id.internal.jackson.Ulid128JacksonModule;
import com.bluecone.app.id.internal.metrics.UlidMetrics;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.id.internal.publicid.DefaultPublicIdCodec;
import com.fasterxml.jackson.databind.Module;

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
    @ConditionalOnProperty(prefix = "bluecone.id.long", name = "enabled", havingValue = "true")
    public SnowflakeLongIdGenerator snowflakeLongIdGenerator(BlueconeIdProperties props,
                                                             Environment environment) {
        if (props == null || props.getLong() == null || !props.getLong().isEnabled()) {
            return new SnowflakeLongIdGenerator(0L, 0L);
        }
        LongId longProps = props.getLong();
        long nodeId = InstanceNodeIdProvider.resolveNodeId(longProps, environment);
        return new SnowflakeLongIdGenerator(longProps.getEpochMillis(), nodeId);
    }

    @Bean
    @ConditionalOnMissingBean(PublicIdCodec.class)
    @ConditionalOnProperty(prefix = "bluecone.id.public-id", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PublicIdCodec publicIdCodec(BlueconeIdProperties props) {
        BlueconeIdProperties.PublicId publicIdProps = props != null ? props.getPublicId() : null;
        return new DefaultPublicIdCodec(publicIdProps);
    }

    @Bean
    @ConditionalOnMissingBean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(PublicIdCodec.class)
    public PublicIdFactory publicIdFactory(PublicIdCodec codec) {
        return new PublicIdFactory(codec);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdService idService(UlidIdGenerator ulidIdGenerator,
                               ObjectProvider<SnowflakeLongIdGenerator> longIdGeneratorProvider,
                               ObjectProvider<PublicIdFactory> publicIdFactoryProvider) {
        SnowflakeLongIdGenerator longIdGenerator = longIdGeneratorProvider.getIfAvailable();
        PublicIdFactory publicIdFactory = publicIdFactoryProvider.getIfAvailable();
        return new UlidIdService(ulidIdGenerator, longIdGenerator, publicIdFactory);
    }

    /**
     * Ulid128 Jackson Module Bean（兼容旧版配置方式）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "ulid128JacksonModule")
    public Module ulid128JacksonModule() {
        return new Ulid128JacksonModule();
    }
}
