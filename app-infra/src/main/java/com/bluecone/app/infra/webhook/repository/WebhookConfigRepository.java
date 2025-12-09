package com.bluecone.app.infra.webhook.repository;

import com.bluecone.app.infra.webhook.entity.WebhookConfigDO;
import java.util.List;
import java.util.Optional;

/**
 * Webhook 配置仓储接口。
 */
public interface WebhookConfigRepository {

    /**
     * 根据租户 + 事件类型 获取已启用的 Webhook 配置。
     *
     * @param tenantId  租户 ID
     * @param eventType 事件类型
     * @return 已启用的 Webhook 配置，如果不存在则返回 empty
     */
    Optional<WebhookConfigDO> findEnabledWebhook(Long tenantId, String eventType);

    /**
     * 查询租户下的所有配置。
     */
    List<WebhookConfigDO> listByTenant(Long tenantId);

    /**
     * 根据 ID 查找配置。
     */
    Optional<WebhookConfigDO> findById(Long id);

    /**
     * 保存配置。
     */
    void save(WebhookConfigDO config);

    /**
     * 更新配置。
     */
    void update(WebhookConfigDO config);

    /**
     * 删除配置。
     */
    void deleteById(Long id);
}
