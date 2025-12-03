package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建门店的写侧命令对象。
 * <p>应用层负责参数校验与用例编排，领域层聚合负责一致性控制。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoreCommand {
    private Long tenantId;
    private String storeCode;
    private String name;
    private String shortName;
    private String industryType;
    private String cityCode;
    private Boolean openForOrders;
}
