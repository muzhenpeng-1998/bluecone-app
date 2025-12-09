package com.bluecone.app.store.domain.model.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店运行时快照（最小版），用于快速判断接单能力，不携带持久化/ORM 注解。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreRuntime {

    /** 租户 ID，保持与门店表字段类型一致。 */
    private Long tenantId;

    /** 门店 ID。 */
    private Long storeId;

    /** 门店名称。 */
    private String storeName;

    /**
     * 业务状态（示例：1=营业，0=关闭，-1=审核中）。
     * 目前底表为字符串 status，待引入明确枚举后再统一映射。
     */
    private Integer bizStatus;

    /** 是否被运营后台强制打烊/暂停接单。 */
    private Boolean forceClosed;

    /** 是否支持外卖（底表若无对应字段先留空，后续补映射）。 */
    private Boolean takeoutEnabled;

    /** 是否支持自提。 */
    private Boolean pickupEnabled;

    /** 是否支持堂食。 */
    private Boolean dineInEnabled;

    /** 预留扩展字段（JSON）。 */
    private String extJson;
}

