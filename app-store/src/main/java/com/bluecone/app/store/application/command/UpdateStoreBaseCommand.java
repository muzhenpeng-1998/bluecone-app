package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新门店基础信息的写侧命令，应用层用例编排入口。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreBaseCommand {
    private Long tenantId;
    private Long storeId;
    private String name;
    private String shortName;
    private String industryType;
    private String cityCode;
    private Boolean openForOrders;
    private Long expectedConfigVersion;
}
