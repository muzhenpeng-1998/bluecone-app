package com.bluecone.app.ops.config;

import com.bluecone.app.ops.security.OpsConsoleAccessInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the ops console interceptor for /ops/** endpoints.
 */
@Configuration
public class OpsWebMvcConfiguration implements WebMvcConfigurer {

    private final BlueconeOpsProperties properties;

    public OpsWebMvcConfiguration(final BlueconeOpsProperties properties) {
        this.properties = properties;
    }

    @Bean
    public OpsConsoleAccessInterceptor opsConsoleAccessInterceptor() {
        return new OpsConsoleAccessInterceptor(properties);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(opsConsoleAccessInterceptor())
                .addPathPatterns("/ops/**");
    }
}

