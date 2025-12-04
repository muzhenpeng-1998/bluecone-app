package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调整打印规则状态的命令。
 * <p>高隔离：只描述输入契约，供应用层统一处理。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStorePrintRuleStatusCommand {

    private Long tenantId;
    private Long storeId;
    private Long ruleId;
    private String targetStatus;
    private Long operatorId;
}
