package com.bluecone.app.billing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订阅计费定时任务配置
 */
@Configuration
@EnableScheduling
public class BillingSchedulerConfig {
    // 启用 Spring 定时任务支持
    // SubscriptionExpireJob 和 BillingReconcileJob 将自动执行
}
