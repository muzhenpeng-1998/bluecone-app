package com.bluecone.app.id.autoconfigure;

import java.time.Clock;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.config.BlueconeIdProperties;
import com.bluecone.app.id.config.InstanceNodeIdProvider;
import com.bluecone.app.id.core.PublicIdFactory;
import com.bluecone.app.id.core.SnowflakeLongIdGenerator;
import com.bluecone.app.id.core.UlidIdGenerator;
import com.bluecone.app.id.core.UlidIdService;
import com.bluecone.app.id.metrics.UlidMetrics;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.id.publicid.core.DefaultPublicIdCodec;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * ID 模块的自动装配配置，通过 Spring Boot 3.x AutoConfiguration 机制生效。
 *
 * <p>引入 app-id 依赖后即可自动装配 UlidIdGenerator 与 IdService，
 * 并可通过 {@code bluecone.id.*} 进行配置。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(BlueconeIdProperties.class)
@ConditionalOnProperty(prefix = "bluecone.id", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdAutoConfiguration {

    /**
     * 装配 ULID 生成器。
     *
     * @param props                 ID 配置属性
     * @param clockProvider         可选 Clock Bean
     * @param meterRegistryProvider 可选指标注册中心
     * @return ULID 生成器
     */
    @Bean
    @ConditionalOnMissingBean
    public UlidIdGenerator ulidIdGenerator(BlueconeIdProperties props,
                                           ObjectProvider<Clock> clockProvider,
                                           ObjectProvider<MeterRegistry> meterRegistryProvider) {
        int stripes;
        BlueconeIdProperties.Mode mode = BlueconeIdProperties.Mode.STRIPED;
        BlueconeIdProperties.Ulid ulidProps = null;

        if (props == null || !props.isEnabled()) {
            // 若整体未启用，则不应进入本配置；此处兜底为 STRICT
            stripes = 1;
            mode = BlueconeIdProperties.Mode.STRICT;
        } else {
            ulidProps = props.getUlid();
            if (ulidProps == null) {
                stripes = 1;
                mode = BlueconeIdProperties.Mode.STRICT;
            } else if (ulidProps.getMode() == BlueconeIdProperties.Mode.STRICT) {
                stripes = 1;
                mode = BlueconeIdProperties.Mode.STRICT;
            } else {
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

        Clock clock = clockProvider.getIfAvailable(() -> Clock.systemUTC());

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

    /**
     * 装配 Snowflake long ID 生成器。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "bluecone.id.long", name = "enabled", havingValue = "true")
    public SnowflakeLongIdGenerator snowflakeLongIdGenerator(BlueconeIdProperties props,
                                                             Environment environment) {
        if (props == null || props.getLong() == null || !props.getLong().isEnabled()) {
            // 按条件注解，此分支理论上不会被命中，兜底返回一个禁用配置
            return new SnowflakeLongIdGenerator(0L, 0L);
        }
        BlueconeIdProperties.LongId longProps = props.getLong();
        long nodeId = InstanceNodeIdProvider.resolveNodeId(longProps, environment);
        return new SnowflakeLongIdGenerator(longProps.getEpochMillis(), nodeId);
    }

    /**
     * 装配统一 ID 服务门面。
     *
     * @param ulidIdGenerator ULID 生成器
     * @return IdService 实例
     */
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
     * 装配 PublicIdCodec，用于公开 ID 的统一编码与解析。
     */
    @Bean
    @ConditionalOnMissingBean(PublicIdCodec.class)
    @ConditionalOnProperty(prefix = "bluecone.id.public-id", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PublicIdCodec publicIdCodec(BlueconeIdProperties props) {
        BlueconeIdProperties.PublicId publicIdProps = props != null ? props.getPublicId() : null;
        return new DefaultPublicIdCodec(publicIdProps);
    }

    /**
     * 装配 PublicIdFactory，基于 PublicIdCodec 生成字符串形式的 PublicId。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(PublicIdCodec.class)
    public PublicIdFactory publicIdFactory(PublicIdCodec codec) {
        return new PublicIdFactory(codec);
    }
}
