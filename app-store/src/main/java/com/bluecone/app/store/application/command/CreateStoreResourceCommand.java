package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建门店资源的命令（餐桌/包间/场地等）。
 * <p>高并发：资源作为后续占用/预订基础，需带租户/门店标识。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoreResourceCommand {

    private Long tenantId;
    private Long storeId;
    private String resourceType;
    private String code;
    private String name;
    private String area;
    private String metadataJson;
    private Long operatorId;
}
