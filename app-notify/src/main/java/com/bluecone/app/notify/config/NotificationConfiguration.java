package com.bluecone.app.notify.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 通知模块配置
 */
@Configuration
@EnableScheduling
public class NotificationConfiguration {
    
    @Bean
    public ObjectMapper notificationObjectMapper() {
        return new ObjectMapper();
    }
}
