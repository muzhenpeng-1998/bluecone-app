package com.bluecone.app.infra.webhook.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.infra.webhook.entity.WebhookConfigDO;
import com.bluecone.app.infra.webhook.mapper.WebhookConfigMapper;
import com.bluecone.app.infra.webhook.repository.WebhookConfigRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Webhook 配置仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class WebhookConfigRepositoryImpl implements WebhookConfigRepository {

    private final WebhookConfigMapper webhookConfigMapper;

    @Override
    public Optional<WebhookConfigDO> findEnabledWebhook(Long tenantId, String eventType) {
        WebhookConfigDO config = webhookConfigMapper.selectOne(
                new LambdaQueryWrapper<WebhookConfigDO>()
                        .eq(WebhookConfigDO::getTenantId, tenantId)
                        .eq(WebhookConfigDO::getEventType, eventType)
                        .eq(WebhookConfigDO::getEnabled, 1)
        );
        return Optional.ofNullable(config);
    }

    @Override
    public List<WebhookConfigDO> listByTenant(Long tenantId) {
        return webhookConfigMapper.selectList(
                new LambdaQueryWrapper<WebhookConfigDO>()
                        .eq(WebhookConfigDO::getTenantId, tenantId)
                        .orderByDesc(WebhookConfigDO::getCreatedAt)
        );
    }

    @Override
    public Optional<WebhookConfigDO> findById(Long id) {
        return Optional.ofNullable(webhookConfigMapper.selectById(id));
    }

    @Override
    public void save(WebhookConfigDO config) {
        webhookConfigMapper.insert(config);
    }

    @Override
    public void update(WebhookConfigDO config) {
        webhookConfigMapper.updateById(config);
    }

    @Override
    public void deleteById(Long id) {
        webhookConfigMapper.deleteById(id);
    }
}
