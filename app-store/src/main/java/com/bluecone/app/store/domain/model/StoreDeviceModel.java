package com.bluecone.app.store.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 对应 bc_store_device，包含打印规则（bc_store_print_rule）的领域模型。
 * <p>在高并发订单场景中可直接从 StoreConfig 快照中读取设备与打印策略，减少额外查询。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDeviceModel {

    private String deviceType;

    private String name;

    private String sn;

    private String configJson;

    private String status;

    private List<StorePrintRuleModel> printRules;
}
