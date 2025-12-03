package com.bluecone.app.store.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店基础信息视图（只读 DTO），用于列表和简单展示，不承载复杂业务逻辑。
 * <p>高隔离：外部模块只接触此视图对象，不暴露领域模型与 Mapper。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreBaseView {

    private Long tenantId;
    private Long storeId;
    private String storeCode;
    private String name;
    private String shortName;
    private String industryType;
    private String cityCode;
    private String status;
    private Boolean openForOrders;
    private String logoUrl;
    private String coverUrl;
}
