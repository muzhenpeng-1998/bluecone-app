package com.bluecone.app.store.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对应 bc_store_print_rule 的领域模型，描述打印触发事件与模板配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorePrintRuleModel {

    private String eventType;

    private Long targetDeviceId;

    private String templateCode;

    private String configJson;
}
