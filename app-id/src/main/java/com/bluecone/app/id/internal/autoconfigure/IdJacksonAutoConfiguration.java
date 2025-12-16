package com.bluecone.app.id.internal.autoconfigure;

import com.bluecone.app.id.internal.jackson.BlueconeIdJacksonModule;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Jackson 相关自动配置：注册 TypedId 等的序列化支持。
 */
@AutoConfiguration(after = IdAutoConfiguration.class)
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(prefix = "bluecone.id.jackson", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdJacksonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "blueconeIdJacksonModule")
    public Module blueconeIdJacksonModule(PublicIdCodec codec) {
        return new BlueconeIdJacksonModule(codec);
    }
}
