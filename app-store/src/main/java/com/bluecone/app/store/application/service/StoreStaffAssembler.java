package com.bluecone.app.store.application.service;

import com.bluecone.app.store.api.dto.StoreStaffView;
import com.bluecone.app.store.dao.entity.BcStoreStaff;
import org.springframework.stereotype.Component;

/**
 * 员工关系装配器：实体 → DTO。
 * <p>高隔离：避免外部直接依赖实体。</p>
 */
@Component
public class StoreStaffAssembler {

    public StoreStaffView toView(BcStoreStaff entity) {
        if (entity == null) {
            return null;
        }
        return StoreStaffView.builder()
                .tenantId(entity.getTenantId())
                .storeId(entity.getStoreId())
                .staffId(entity.getId())
                .userId(entity.getUserId())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC))
                .build();
    }
}
