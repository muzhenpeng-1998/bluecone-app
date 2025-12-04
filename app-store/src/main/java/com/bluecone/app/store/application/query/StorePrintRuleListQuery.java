package com.bluecone.app.store.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 打印规则列表查询对象。
 * <p>支持按事件类型/状态过滤。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorePrintRuleListQuery {

    private Long tenantId;
    private Long storeId;
    private String eventType;
    private String status;
}
