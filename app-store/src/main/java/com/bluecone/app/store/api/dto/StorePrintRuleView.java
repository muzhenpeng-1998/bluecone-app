package com.bluecone.app.store.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店打印规则视图（事件 → 设备 → 模板），只读 DTO。
 * <p>高隔离：对外只暴露规则数据，不包含内部实体或路由细节。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorePrintRuleView {

    private Long tenantId;
    private Long storeId;
    private Long ruleId;
    private String eventType;
    private Long targetDeviceId;
    private String templateCode;
    private String configJson;
    private String status;
}
