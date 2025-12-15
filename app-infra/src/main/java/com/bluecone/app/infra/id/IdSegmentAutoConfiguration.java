package com.bluecone.app.infra.id;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.config.BlueconeIdProperties;
import com.bluecone.app.id.core.EnhancedIdService;
import com.bluecone.app.id.core.PublicIdFactory;
import com.bluecone.app.id.core.UlidIdGenerator;
import com.bluecone.app.id.segment.IdSegmentRepository;
import com.bluecone.app.id.segment.SegmentLongIdGenerator;
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
 *   <li>bluecone.id.segment.enabled=true（默认 true）</li>
 *   <li>存在 JdbcTemplate Bean（Spring Boot JDBC 自动装配）</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(BlueconeIdProperties.class)
@ConditionalOnProperty(prefix = "bluecone.id.segment", name = "enabled", havingValue = "true", matchIfMissing = true)
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
        return new EnhancedIdService(ulidIdGenerator, segmentLongIdGenerator, publicIdFactory);
    }
}

