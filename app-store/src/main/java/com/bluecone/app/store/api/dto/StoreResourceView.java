package com.bluecone.app.store.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店资源视图（餐桌/包间/场地等），只读 DTO。
 * <p>高隔离：外部只依赖此视图，领域实现细节隐藏。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResourceView {

    private Long tenantId;
    private Long storeId;
    private Long resourceId;
    private String resourceType;
    private String code;
    private String name;
    private String area;
    private String status;
    private String metadataJson;
    private String metadataSummary;
}
