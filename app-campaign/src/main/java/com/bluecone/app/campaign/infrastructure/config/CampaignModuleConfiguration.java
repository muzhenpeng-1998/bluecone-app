package com.bluecone.app.campaign.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 活动模块配置
 */
@Configuration
@ComponentScan("com.bluecone.app.campaign")
@MapperScan("com.bluecone.app.campaign.infrastructure.persistence.mapper")
public class CampaignModuleConfiguration {
}
