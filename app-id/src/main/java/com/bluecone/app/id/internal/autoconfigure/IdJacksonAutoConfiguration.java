package com.bluecone.app.id.internal.autoconfigure;

import com.bluecone.app.id.internal.jackson.BlueconeIdJacksonModule;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
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

    /**
     * 装配 BlueconeId Jackson 模块。
     * 
     * <p>使用 ObjectProvider 避免在 PublicIdCodec 未启用时导致启动失败。
     */
    @Bean
    @ConditionalOnMissingBean(name = "blueconeIdJacksonModule")
    public Module blueconeIdJacksonModule(ObjectProvider<PublicIdCodec> codecProvider) {
        PublicIdCodec codec = codecProvider.getIfAvailable();
        return new BlueconeIdJacksonModule(codec);
    }
}
