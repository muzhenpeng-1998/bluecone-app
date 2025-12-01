package com.bluecone.app.infra.notify.delivery;

import com.bluecone.app.infra.notify.policy.NotifyChannel;

/**
 * 通道插件接口（Delivery 层）。
 */
public interface NotificationChannel {

    /**
     * @return 通道类型
     */
    NotifyChannel channel();

    /**
     * 执行具体投递。
     *
     * @param envelope 包含意图与任务
     * @return 投递结果
     */
    DeliveryResult deliver(NotificationEnvelope envelope);
}
