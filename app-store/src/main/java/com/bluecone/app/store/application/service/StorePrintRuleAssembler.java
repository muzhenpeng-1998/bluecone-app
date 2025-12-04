package com.bluecone.app.store.application.service;

import com.bluecone.app.store.api.dto.StorePrintRuleView;
import com.bluecone.app.store.dao.entity.BcStorePrintRule;
import org.springframework.stereotype.Component;

/**
 * 打印规则装配器：实体 → DTO。
 * <p>高隔离：外部仅见视图对象。</p>
 */
@Component
public class StorePrintRuleAssembler {

    public StorePrintRuleView toView(BcStorePrintRule entity) {
        if (entity == null) {
            return null;
        }
        return StorePrintRuleView.builder()
                .tenantId(entity.getTenantId())
                .storeId(entity.getStoreId())
                .ruleId(entity.getId())
                .eventType(entity.getEventType())
                .targetDeviceId(entity.getTargetDeviceId())
                .templateCode(entity.getTemplateCode())
                .configJson(entity.getConfigJson())
                .status(null) // entity 未包含状态字段，预留扩展
                .build();
    }
}
