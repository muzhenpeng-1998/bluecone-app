package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新门店资源信息的命令。
 * <p>高稳定：仅修改必要字段，租户/门店/资源主键需显式传入。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreResourceCommand {

    private Long tenantId;
    private Long storeId;
    private Long resourceId;
    private String name;
    private String area;
    private String metadataJson;
    private Long operatorId;
}
