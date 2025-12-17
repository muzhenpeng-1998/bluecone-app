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
                .metadataSummary(parseMetadataSummary(entity.getMetadataJson(), entity.getResourceType())) // 解析元数据摘要：提取资源类型或 JSON 关键信息
                .build();
    }

    /**
     * 解析资源元数据摘要。
     * <p>最小实现：优先返回资源类型，如无则返回 JSON 前 50 字符。</p>
     *
     * @param metadataJson 元数据 JSON 字符串
     * @param resourceType 资源类型（如 IMAGE、VIDEO、DOCUMENT 等）
     * @return 元数据摘要（如为 null 或空则返回资源类型）
     */
    private String parseMetadataSummary(String metadataJson, String resourceType) {
        // 优先返回资源类型作为摘要
        if (resourceType != null && !resourceType.isBlank()) {
            return resourceType;
        }
        // 如无资源类型，则返回 JSON 前 50 字符
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        // 最小实现：返回 JSON 前 50 字符作为摘要
        // 后续可扩展为解析 JSON 并提取关键字段（如文件大小、URL、格式等）
        return metadataJson.length() > 50 ? metadataJson.substring(0, 50) + "..." : metadataJson;
    }
}
