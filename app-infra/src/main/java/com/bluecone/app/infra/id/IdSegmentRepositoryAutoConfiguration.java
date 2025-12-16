package com.bluecone.app.infra.id;

import com.bluecone.app.id.segment.IdSegmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ID 号段仓储自动装配配置（app-infra 模块）。
 *
 * <p>职责：仅提供 IdSegmentRepository 的 JDBC 实现 Bean，
 * 不涉及任何 ID 生成器或 IdService 的装配（由 app-id 模块负责）。
 *
 * <p>启用条件：存在 JdbcTemplate Bean（Spring Boot JDBC 自动装配）。
 */
@AutoConfiguration
@ConditionalOnBean(JdbcTemplate.class)
public class IdSegmentRepositoryAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(IdSegmentRepositoryAutoConfiguration.class);

    /**
     * 装配 JDBC 号段仓储。
     *
     * @param jdbcTemplate Spring JdbcTemplate
     * @return IdSegmentRepository 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public IdSegmentRepository idSegmentRepository(JdbcTemplate jdbcTemplate) {
        log.info("装配 JdbcIdSegmentRepository（app-infra 提供 SPI 实现）");
        return new JdbcIdSegmentRepository(jdbcTemplate);
    }
}

