package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量新增/更新门店打印规则的命令。
 * <p>高稳定：前端一次提交全量规则，由后端做 upsert。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertStorePrintRulesCommand {

    private Long tenantId;
    private Long storeId;
    private List<PrintRuleItem> rules;
    private Long operatorId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrintRuleItem {
        private Long ruleId;
        private String eventType;
        private Long targetDeviceId;
        private String templateCode;
        private String configJson;
        private String status;
    }
}
