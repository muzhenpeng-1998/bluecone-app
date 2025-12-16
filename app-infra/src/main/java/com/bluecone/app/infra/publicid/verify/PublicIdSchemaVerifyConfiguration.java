package com.bluecone.app.infra.publicid.verify;

import com.bluecone.app.infra.publicid.config.PublicIdResourceDefinitionLoader;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Public ID 索引校验自动配置。
 */
@Configuration
@EnableConfigurationProperties(PublicIdVerifyProperties.class)
@ConditionalOnClass(DataSource.class)
public class PublicIdSchemaVerifyConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PublicIdResourceDefinitionLoader publicIdResourceDefinitionLoader() {
        return new PublicIdResourceDefinitionLoader();
    }

    @Bean
    @ConditionalOnMissingBean
    public PublicIdSchemaVerifier publicIdSchemaVerifier(PublicIdVerifyProperties properties,
                                                        PublicIdResourceDefinitionLoader loader,
                                                        DataSource dataSource) {
        return new PublicIdSchemaVerifier(properties, loader, dataSource);
    }
}

