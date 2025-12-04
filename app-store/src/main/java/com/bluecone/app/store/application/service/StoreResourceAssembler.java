package com.bluecone.app.store.application.service;

import com.bluecone.app.store.api.dto.StoreResourceView;
import com.bluecone.app.store.dao.entity.BcStoreResource;
import org.springframework.stereotype.Component;

/**
 * 资源装配器：实体 → 对外视图 DTO。
 * <p>高隔离：隐藏实体结构，便于后续调整。</p>
 */
@Component
public class StoreResourceAssembler {

    public StoreResourceView toView(BcStoreResource entity) {
        if (entity == null) {
            return null;
        }
        return StoreResourceView.builder()
                .tenantId(entity.getTenantId())
                .storeId(entity.getStoreId())
                .resourceId(entity.getId())
                .resourceType(entity.getResourceType())
                .code(entity.getCode())
                .name(entity.getName())
                .area(entity.getArea())
                .status(entity.getStatus())
                .metadataJson(entity.getMetadataJson())
                .metadataSummary(null) // TODO: 元数据摘要可后续补充
                .build();
    }
}
