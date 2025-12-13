package com.bluecone.app.migration.id;

import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ID 回填 Job 装配配置。
 */
@Configuration
@EnableConfigurationProperties(IdBackfillProperties.class)
public class IdBackfillConfiguration {

    @Bean
    @ConditionalOnBean({JdbcTemplate.class, IdService.class, PublicIdCodec.class})
    @ConditionalOnProperty(prefix = "bluecone.migration.id-backfill", name = "enabled", havingValue = "true")
    public IdBackfillRunner idBackfillRunner(JdbcTemplate jdbcTemplate,
                                             IdService idService,
                                             PublicIdCodec publicIdCodec,
                                             IdBackfillProperties properties) {
        return new IdBackfillRunner(jdbcTemplate, idService, publicIdCodec, properties);
    }
}

