package com.bluecone.app.campaign.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 活动模块配置
 * 
 * 注意：Mapper 扫描由全局 MybatisPlusConfig 统一处理，无需在此重复配置
 */
@Configuration
@ComponentScan("com.bluecone.app.campaign")
public class CampaignModuleConfiguration {
}
