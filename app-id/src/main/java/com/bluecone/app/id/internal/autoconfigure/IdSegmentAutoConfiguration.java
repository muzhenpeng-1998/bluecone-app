package com.bluecone.app.id.internal.autoconfigure;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.internal.config.BlueconeIdProperties;
import com.bluecone.app.id.internal.core.EnhancedIdService;
import com.bluecone.app.id.internal.core.PublicIdFactory;
import com.bluecone.app.id.internal.core.UlidIdGenerator;
import com.bluecone.app.id.internal.segment.SegmentLongIdGenerator;
import com.bluecone.app.id.segment.IdSegmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * ID 号段模式自动装配配置（app-id 模块）。
 *
 * <p>负责在容器中发现 IdSegmentRepository Bean 后，装配 SegmentLongIdGenerator
 * 并升级 IdService 为 EnhancedIdService（支持 ULID + Segment Long ID + Public ID）。
 *
 * <p>启用条件：
 * <ul>
 *   <li>bluecone.id.long.strategy=SEGMENT（默认值）</li>
 *   <li>容器中存在 IdSegmentRepository Bean（由 app-infra 提供 JDBC 实现）</li>
 * </ul>
 *
 * <p>设计原则：app-id 模块不实现 repository，只做装配与拼装；
 * repository 的 JDBC 实现由 app-infra 提供。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(BlueconeIdProperties.class)
@ConditionalOnProperty(prefix = "bluecone.id.long", name = "strategy", havingValue = "SEGMENT", matchIfMissing = true)
@ConditionalOnBean(IdSegmentRepository.class)
public class IdSegmentAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(IdSegmentAutoConfiguration.class);

    /**
     * 装配号段 Long ID 生成器。
     *
     * @param repository 号段仓储（由 app-infra 提供）
     * @param propsProvider ID 配置属性
     * @return SegmentLongIdGenerator 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public SegmentLongIdGenerator segmentLongIdGenerator(IdSegmentRepository repository,
                                                         ObjectProvider<BlueconeIdProperties> propsProvider) {
        BlueconeIdProperties props = propsProvider.getIfAvailable();
        int step = 1000; // 默认步长
        if (props != null && props.getSegment() != null) {
            step = props.getSegment().getStep();
        }

        log.info("装配 SegmentLongIdGenerator，step={}", step);

        // 初始化所有 scope（幂等操作）
        for (IdScope scope : IdScope.values()) {
            try {
                repository.initScopeIfAbsent(scope, 0L, step);
            } catch (Exception e) {
                log.warn("初始化 scope 失败（可能已存在）: {}", scope, e);
            }
        }

        return new SegmentLongIdGenerator(repository, step);
    }

    /**
     * 装配增强版 IdService（覆盖默认的 UlidIdService）。
     *
     * <p>此配置类优先级高于 IdAutoConfiguration 中的 idService Bean，
     * 当 strategy=SEGMENT 且 IdSegmentRepository 存在时，装配 EnhancedIdService，
     * 提供完整的三层 ID 能力（ULID + Segment Long ID + Public ID）。
     *
     * @param ulidIdGenerator ULID 生成器
     * @param segmentLongIdGenerator 号段 Long ID 生成器
     * @param publicIdFactoryProvider Public ID 工厂
     * @return EnhancedIdService 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "idService")
    @ConditionalOnBean({UlidIdGenerator.class, SegmentLongIdGenerator.class})
    public IdService idService(UlidIdGenerator ulidIdGenerator,
                               SegmentLongIdGenerator segmentLongIdGenerator,
                               ObjectProvider<PublicIdFactory> publicIdFactoryProvider) {
        PublicIdFactory publicIdFactory = publicIdFactoryProvider.getIfAvailable();
        log.info("装配 EnhancedIdService（支持 ULID + Segment Long ID + Public ID）");
        log.info("Long ID 策略：SEGMENT（号段模式）");
        return new EnhancedIdService(ulidIdGenerator, segmentLongIdGenerator, publicIdFactory);
    }

    /**
     * 号段模式启动检查器，确保必要的 repository Bean 存在。
     */
    @Bean
    public IdSegmentStartupValidator idSegmentStartupValidator(IdSegmentRepository repository) {
        return new IdSegmentStartupValidator(repository);
    }

    /**
     * 启动检查器，验证号段模式的必要条件。
     */
    public static class IdSegmentStartupValidator {

        private static final Logger validatorLog = LoggerFactory.getLogger(IdSegmentStartupValidator.class);

        public IdSegmentStartupValidator(IdSegmentRepository repository) {
            if (repository == null) {
                String errorMsg = "Long ID 策略配置为 SEGMENT（号段模式），但未找到 IdSegmentRepository Bean。\n"
                        + "请确保：\n"
                        + "1. 数据库中存在 bc_id_segment 表（参考 docs/sql/bc_id_segment.sql）\n"
                        + "2. app-infra 模块已正确配置 JdbcTemplate 并提供 JdbcIdSegmentRepository Bean\n"
                        + "3. 或者切换到 SNOWFLAKE 策略：bluecone.id.long.strategy=SNOWFLAKE";
                validatorLog.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            validatorLog.info("号段模式启动检查通过：IdSegmentRepository Bean 已就绪");
        }
    }
}

