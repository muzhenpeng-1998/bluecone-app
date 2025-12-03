package com.bluecone.app.store.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对应 bc_store_capability 的领域模型，描述门店开通的业务能力（堂食、外卖、自取、预约、场馆等）。
 * <p>高并发场景下作为 StoreConfig 的子视图被整体缓存，避免零散查询。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreCapabilityModel {

    /**
     * 能力标识，例如 TAKE_OUT、DINE_IN。
     */
    private String capability;

    /**
     * 是否启用该能力。
     */
    private Boolean enabled;

    /**
     * 业务配置 JSON，可按能力定义扩展。
     */
    private String configJson;
}
