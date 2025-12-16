package com.bluecone.app.infra.id;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.internal.config.BlueconeIdProperties;
import com.bluecone.app.id.internal.core.EnhancedIdService;
import com.bluecone.app.id.internal.core.PublicIdFactory;
import com.bluecone.app.id.internal.core.UlidIdGenerator;
import com.bluecone.app.id.segment.IdSegmentRepository;
import com.bluecone.app.id.internal.segment.SegmentLongIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ID 号段模式自动装配配置（app-infra 模块）。
 * 
 * <p>提供基于 JDBC 的号段仓储实现，并装配增强版 IdService。
 * 
 * <p>启用条件：
 * <ul>
 *   <li>bluecone.id.long.strategy=SEGMENT（默认值）</li>
 *   <li>存在 JdbcTemplate Bean（Spring Boot JDBC 自动装配）</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(BlueconeIdProperties.class)
@ConditionalOnProperty(prefix = "bluecone.id.long", name = "strategy", havingValue = "SEGMENT", matchIfMissing = true)
@ConditionalOnBean(JdbcTemplate.class)
public class IdSegmentAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(IdSegmentAutoConfiguration.class);
    
    /**
     * 装配 JDBC 号段仓储。
     * 
     * @param jdbcTemplate Spring JdbcTemplate
     * @return IdSegmentRepository 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public IdSegmentRepository idSegmentRepository(JdbcTemplate jdbcTemplate) {
        log.info("装配 JdbcIdSegmentRepository");
        return new JdbcIdSegmentRepository(jdbcTemplate);
    }
    
    /**
     * 装配号段 Long ID 生成器。
     * 
     * @param repository 号段仓储
     * @param props ID 配置属性
     * @return SegmentLongIdGenerator 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public SegmentLongIdGenerator segmentLongIdGenerator(IdSegmentRepository repository,
                                                         BlueconeIdProperties props) {
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
     * <p>优先级高于 app-id 模块的默认 IdService，提供完整的三层 ID 能力。
     * 
     * <p>当 strategy=SEGMENT 时，此 Bean 会装配 EnhancedIdService，使用 Segment 生成器。
     * 若 IdSegmentRepository Bean 不存在，Spring Boot 启动会失败（通过 @ConditionalOnBean 保证）。
     * 
     * @param ulidIdGenerator ULID 生成器
     * @param segmentLongIdGenerator 号段 Long ID 生成器
     * @param publicIdFactory Public ID 工厂
     * @return EnhancedIdService 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "idService")
    @ConditionalOnBean({UlidIdGenerator.class, SegmentLongIdGenerator.class, PublicIdFactory.class})
    public IdService idService(UlidIdGenerator ulidIdGenerator,
                               SegmentLongIdGenerator segmentLongIdGenerator,
                               PublicIdFactory publicIdFactory) {
        log.info("装配 EnhancedIdService（支持 ULID + Segment Long ID + Public ID）");
        log.info("Long ID 策略：SEGMENT（号段模式）");
        return new EnhancedIdService(ulidIdGenerator, segmentLongIdGenerator, publicIdFactory);
    }
    
    /**
     * 号段模式启动检查器，确保必要的数据库表存在。
     * 
     * <p>当 strategy=SEGMENT 但 IdSegmentRepository Bean 不存在时，给出清晰的错误信息。
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
                        + "2. app-infra 模块已正确配置 JdbcTemplate\n"
                        + "3. 或者切换到 SNOWFLAKE 策略：bluecone.id.long.strategy=SNOWFLAKE";
                validatorLog.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            validatorLog.info("号段模式启动检查通过：IdSegmentRepository Bean 已就绪");
        }
    }
}

